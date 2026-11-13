package com.projectx.tracking.ws;

import com.projectx.common.security.CurrentUser;
import com.projectx.common.security.Role;
import com.projectx.people.domain.Child;
import com.projectx.people.domain.ParentProfile;
import com.projectx.people.repo.ChildRepository;
import com.projectx.people.repo.DriverProfileRepository;
import com.projectx.people.repo.ParentProfileRepository;
import com.projectx.school.domain.Route;
import com.projectx.school.domain.Vehicle;
import com.projectx.school.repo.RouteRepository;
import com.projectx.school.repo.VehicleRepository;
import com.projectx.tracking.domain.Trip;
import com.projectx.tracking.repo.TripRepository;
import java.security.Principal;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.stereotype.Component;

/**
 * The native WebSocket handshake can't carry an Authorization header, so instead we require the
 * access token on the STOMP CONNECT frame itself (a header the client sets regardless of
 * transport) and reject the connection if it's missing/invalid.
 */
@Component
public class WebSocketAuthChannelInterceptor implements ChannelInterceptor {

    /** The only arrival-topic shape that is ever allowed: exact literal prefix/suffix and a plain positive numeric id. */
    private static final Pattern ARRIVAL_TOPIC = Pattern.compile("^/topic/(parent|school|trip)/([1-9][0-9]{0,17})/arrivals$");
    /** The only teacher-alert topic shape that is ever allowed: /topic/teacher/{positive id}/alerts, exact match. */
    private static final Pattern TEACHER_ALERT_TOPIC = Pattern.compile("^/topic/teacher/([1-9][0-9]{0,17})/alerts$");
    private static final String TEACHER_NAMESPACE = "/topic/teacher/";
    /** Namespaces owned by the arrival feature: anything under them that is not exactly ARRIVAL_TOPIC is denied. */
    private static final List<String> ARRIVAL_NAMESPACES = List.of("/topic/parent/", "/topic/school/", "/topic/trip/");

    /**
     * The vehicle-location, trip-location/SOS and grievance topics (see {@code LiveLocationBroadcaster},
     * {@code SosBroadcaster}, {@code GrievanceBroadcaster}) used to be reachable by ANY authenticated
     * user for ANY id - a logged-in parent could subscribe to `/topic/vehicles/{anyone's vehicle}/location`
     * or `/topic/schools/{any other school}/grievances` and see live GPS and grievance data that has
     * nothing to do with them. These are now deny-by-default, mirroring the arrival-topic pattern above,
     * with the SAME authorization rules the equivalent REST endpoints already enforce - see
     * LiveLocationQueryService (vehicle/trip), GrievanceController (grievances).
     */
    private static final Pattern VEHICLE_LOCATION_TOPIC = Pattern.compile("^/topic/vehicles/([1-9][0-9]{0,17})/location$");
    private static final Pattern TRIP_LOCATION_TOPIC = Pattern.compile("^/topic/trips/([1-9][0-9]{0,17})/location$");
    private static final Pattern TRIP_SOS_TOPIC = Pattern.compile("^/topic/trips/([1-9][0-9]{0,17})/sos$");
    private static final Pattern USER_GRIEVANCES_TOPIC = Pattern.compile("^/topic/users/([1-9][0-9]{0,17})/grievances$");
    private static final Pattern SCHOOL_GRIEVANCES_TOPIC = Pattern.compile("^/topic/schools/([1-9][0-9]{0,17})/grievances$");
    private static final String PLATFORM_ADMIN_GRIEVANCES_TOPIC = "/topic/platform-admin/grievances";
    private static final List<String> VEHICLE_NAMESPACES = List.of("/topic/vehicles/", "/topic/trips/");
    private static final List<String> GRIEVANCE_NAMESPACES = List.of("/topic/users/", "/topic/schools/");

    private final JwtDecoder jwtDecoder;
    private final TripRepository tripRepository;
    private final DriverProfileRepository driverProfileRepository;
    private final VehicleRepository vehicleRepository;
    private final RouteRepository routeRepository;
    private final ChildRepository childRepository;
    private final ParentProfileRepository parentProfileRepository;

    public WebSocketAuthChannelInterceptor(JwtDecoder jwtDecoder, TripRepository tripRepository,
                                           DriverProfileRepository driverProfileRepository,
                                           VehicleRepository vehicleRepository, RouteRepository routeRepository,
                                           ChildRepository childRepository, ParentProfileRepository parentProfileRepository) {
        this.jwtDecoder = jwtDecoder;
        this.tripRepository = tripRepository;
        this.driverProfileRepository = driverProfileRepository;
        this.vehicleRepository = vehicleRepository;
        this.routeRepository = routeRepository;
        this.childRepository = childRepository;
        this.parentProfileRepository = parentProfileRepository;
    }

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        // NOT StompHeaderAccessor.wrap(message): that builds a NEW accessor over a copy of the headers, so a
        // setUser() on it is silently lost and every later frame (SUBSCRIBE) reaches us with no user. The
        // message's own mutable accessor is the one StompSubProtocolHandler reads the session user back from.
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null || accessor.getCommand() == null) {
            return message;
        }
        if (StompCommand.CONNECT.equals(accessor.getCommand()) || StompCommand.STOMP.equals(accessor.getCommand())) {
            String authHeader = accessor.getFirstNativeHeader("Authorization");
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                throw new JwtException("Missing bearer token on STOMP CONNECT");
            }
            Jwt jwt = jwtDecoder.decode(authHeader.substring(7));
            accessor.setUser(toPrincipal(jwt));
        } else if (StompCommand.SUBSCRIBE.equals(accessor.getCommand())) {
            authorizeSubscription(accessor);
        }
        return message;
    }

    /**
     * Safe-arrival topics carry a child's name and whereabouts, so unlike the older location topics they are
     * subscription-authorized, deny-by-default:
     * <ul>
     *   <li>a destination containing any pattern/escape character (* ? { } or a backslash) is rejected outright -
     *       the simple broker would expand it to every matching topic of every tenant (e.g. /topic/**);</li>
     *   <li>anything under /topic/parent/, /topic/school/ or /topic/trip/ must match the exact arrival-topic shape;</li>
     *   <li>parent topic: only that parent's own user id; school topic: SCHOOL_ADMIN/TEACHER of that school or
     *       PLATFORM_ADMIN; trip topic: the driver assigned to that trip or school staff of the trip's school.</li>
     * </ul>
     * Fails closed when the session has no authenticated user. The pre-existing vehicle/SOS/grievance topics
     * are intentionally left as they were (not subscription-authorized) - see the fix-round notes.
     */
    private void authorizeSubscription(StompHeaderAccessor accessor) {
        String destination = accessor.getDestination();
        if (destination == null || destination.isBlank()) {
            throw new AccessDeniedException("Subscription destination required");
        }
        if (containsPatternOrControlChars(destination)) {
            throw new AccessDeniedException("Wildcard or malformed subscription destinations are not allowed");
        }
        if (destination.startsWith(TEACHER_NAMESPACE)) {
            authorizeTeacherAlertTopic(accessor, destination);
            return;
        }
        if (destination.equals(PLATFORM_ADMIN_GRIEVANCES_TOPIC)) {
            requireRole(accessor, destination, Role.PLATFORM_ADMIN);
            return;
        }
        if (VEHICLE_NAMESPACES.stream().anyMatch(destination::startsWith)) {
            authorizeVehicleOrTripLiveTopic(accessor, destination);
            return;
        }
        if (GRIEVANCE_NAMESPACES.stream().anyMatch(destination::startsWith)) {
            authorizeGrievanceTopic(accessor, destination);
            return;
        }
        if (ARRIVAL_NAMESPACES.stream().noneMatch(destination::startsWith)) {
            return;
        }
        Matcher m = ARRIVAL_TOPIC.matcher(destination);
        if (!m.matches()) {
            throw new AccessDeniedException("Not a valid arrival topic: " + destination);
        }
        if (!(accessor.getUser() instanceof StompPrincipal principal)) {
            throw new AccessDeniedException("Authentication required to subscribe to " + destination);
        }
        CurrentUser user = principal.currentUser();
        Long id = Long.valueOf(m.group(2));
        boolean allowed = switch (m.group(1)) {
            case "parent" -> user.userId().equals(id);
            case "school" -> isArrivalStaff(user) && user.belongsToSchool(id);
            case "trip" -> canSeeTrip(user, id);
            default -> false;
        };
        if (!allowed) {
            throw new AccessDeniedException("Not allowed to subscribe to " + destination);
        }
    }

    /**
     * Deny-by-default for everything under /topic/teacher/: it must be exactly /topic/teacher/{userId}/alerts, the session must
     * be authenticated, a TEACHER, and {userId} must be that teacher's own id (no one else - not even admins - may listen in).
     */
    private void authorizeTeacherAlertTopic(StompHeaderAccessor accessor, String destination) {
        Matcher m = TEACHER_ALERT_TOPIC.matcher(destination);
        if (!m.matches()) {
            throw new AccessDeniedException("Not a valid teacher alert topic: " + destination);
        }
        if (!(accessor.getUser() instanceof StompPrincipal principal)) {
            throw new AccessDeniedException("Authentication required to subscribe to " + destination);
        }
        CurrentUser user = principal.currentUser();
        if (user.role() != Role.TEACHER || !user.userId().equals(Long.valueOf(m.group(1)))) {
            throw new AccessDeniedException("Not allowed to subscribe to " + destination);
        }
    }

    private static void requireRole(StompHeaderAccessor accessor, String destination, Role required) {
        if (!(accessor.getUser() instanceof StompPrincipal principal) || principal.currentUser().role() != required) {
            throw new AccessDeniedException("Not allowed to subscribe to " + destination);
        }
    }

    /**
     * `/topic/vehicles/{vehicleId}/location`, `/topic/trips/{tripId}/location`, `/topic/trips/{tripId}/sos`.
     * Mirrors LiveLocationQueryService's REST rules exactly: PLATFORM_ADMIN always; SCHOOL_ADMIN/DRIVER if
     * the vehicle/trip belongs to their own school (same as the REST endpoint - this app does not currently
     * restrict a driver to only their own assigned vehicle at this layer, matching the existing REST
     * behavior rather than inventing a stricter rule that the REST API doesn't itself enforce); PARENT if
     * one of their children rides that vehicle/trip's route. Unlike the arrival trip topic (which exposes a
     * whole manifest and deliberately excludes parents), location and SOS are the exact live-tracking data a
     * parent already gets via REST and the UI already subscribes to for their own child - so parents ARE
     * allowed here, scoped to their own child's route only.
     */
    private void authorizeVehicleOrTripLiveTopic(StompHeaderAccessor accessor, String destination) {
        if (!(accessor.getUser() instanceof StompPrincipal principal)) {
            throw new AccessDeniedException("Authentication required to subscribe to " + destination);
        }
        CurrentUser user = principal.currentUser();
        boolean allowed;
        Matcher vehicle = VEHICLE_LOCATION_TOPIC.matcher(destination);
        Matcher tripLocation = TRIP_LOCATION_TOPIC.matcher(destination);
        Matcher tripSos = TRIP_SOS_TOPIC.matcher(destination);
        if (vehicle.matches()) {
            allowed = canSeeVehicle(user, Long.valueOf(vehicle.group(1)));
        } else if (tripLocation.matches()) {
            allowed = canSeeTripLiveData(user, Long.valueOf(tripLocation.group(1)));
        } else if (tripSos.matches()) {
            allowed = canSeeTripLiveData(user, Long.valueOf(tripSos.group(1)));
        } else {
            throw new AccessDeniedException("Not a valid topic: " + destination);
        }
        if (!allowed) {
            throw new AccessDeniedException("Not allowed to subscribe to " + destination);
        }
    }

    private boolean canSeeVehicle(CurrentUser user, Long vehicleId) {
        Vehicle vehicle = vehicleRepository.findById(vehicleId).orElse(null);
        if (vehicle == null) {
            return false;
        }
        if (user.role() == Role.PLATFORM_ADMIN) {
            return true;
        }
        if (user.role() == Role.SCHOOL_ADMIN || user.role() == Role.DRIVER) {
            return user.belongsToSchool(vehicle.getSchoolId());
        }
        if (user.role() == Role.PARENT) {
            return parentHasChildOnVehicle(user.userId(), vehicleId);
        }
        return false;
    }

    private boolean canSeeTripLiveData(CurrentUser user, Long tripId) {
        Trip trip = tripRepository.findById(tripId).orElse(null);
        if (trip == null) {
            return false;
        }
        if (user.role() == Role.DRIVER) {
            return driverProfileRepository.findByUserId(user.userId())
                    .map(driver -> driver.getId().equals(trip.getDriverId())).orElse(false);
        }
        if (isArrivalStaff(user)) {
            return user.belongsToSchool(trip.getSchoolId());
        }
        if (user.role() == Role.PARENT && trip.getRouteId() != null) {
            return parentHasChildOnRoute(user.userId(), trip.getRouteId());
        }
        return false;
    }

    private boolean parentHasChildOnVehicle(Long userId, Long vehicleId) {
        return parentProfileRepository.findByUserId(userId)
                .map(ParentProfile::getId)
                .map(childRepository::findByGuardianParentId)
                .map(children -> children.stream().anyMatch(c -> childRidesVehicle(c, vehicleId)))
                .orElse(false);
    }

    private boolean childRidesVehicle(Child child, Long vehicleId) {
        if (child.getRouteId() == null) {
            return false;
        }
        Route route = routeRepository.findById(child.getRouteId()).orElse(null);
        return route != null && vehicleId.equals(route.getVehicleId());
    }

    private boolean parentHasChildOnRoute(Long userId, Long routeId) {
        return parentProfileRepository.findByUserId(userId)
                .map(ParentProfile::getId)
                .map(childRepository::findByGuardianParentId)
                .map(children -> children.stream().anyMatch(c -> routeId.equals(c.getRouteId())))
                .orElse(false);
    }

    /**
     * `/topic/users/{userId}/grievances` (only that user, any role - mirrors GET /grievances/mine),
     * `/topic/schools/{schoolId}/grievances` (SCHOOL_ADMIN/PLATFORM_ADMIN of that school, mirrors
     * GET /schools/{schoolId}/grievances). `/topic/platform-admin/grievances` is handled separately above.
     */
    private void authorizeGrievanceTopic(StompHeaderAccessor accessor, String destination) {
        if (!(accessor.getUser() instanceof StompPrincipal principal)) {
            throw new AccessDeniedException("Authentication required to subscribe to " + destination);
        }
        CurrentUser user = principal.currentUser();
        Matcher userTopic = USER_GRIEVANCES_TOPIC.matcher(destination);
        Matcher schoolTopic = SCHOOL_GRIEVANCES_TOPIC.matcher(destination);
        boolean allowed;
        if (userTopic.matches()) {
            allowed = user.userId().equals(Long.valueOf(userTopic.group(1)));
        } else if (schoolTopic.matches()) {
            allowed = isArrivalStaff(user) && user.belongsToSchool(Long.valueOf(schoolTopic.group(1)));
        } else {
            throw new AccessDeniedException("Not a valid topic: " + destination);
        }
        if (!allowed) {
            throw new AccessDeniedException("Not allowed to subscribe to " + destination);
        }
    }

    private static boolean containsPatternOrControlChars(String destination) {
        for (int i = 0; i < destination.length(); i++) {
            char c = destination.charAt(i);
            if (c == '*' || c == '?' || c == '{' || c == '}' || c == (char) 92 || c <= ' ' || c == 127) {
                return true;
            }
        }
        return false;
    }

    /** Arrival boards carry every child's whereabouts: school and platform admins only (teachers get their own class alerts instead). */
    private static boolean isArrivalStaff(CurrentUser user) {
        return user.role() == Role.SCHOOL_ADMIN || user.role() == Role.PLATFORM_ADMIN;
    }

    private static boolean isSchoolStaff(CurrentUser user) {
        return user.role() == Role.SCHOOL_ADMIN || user.role() == Role.TEACHER || user.role() == Role.PLATFORM_ADMIN;
    }

    /** The trip's assigned driver, or staff of the trip's school (platform admin included). Parents never. */
    private boolean canSeeTrip(CurrentUser user, Long tripId) {
        Trip trip = tripRepository.findById(tripId).orElse(null);
        if (trip == null) {
            return false;
        }
        if (user.role() == Role.DRIVER) {
            return driverProfileRepository.findByUserId(user.userId())
                    .map(driver -> driver.getId().equals(trip.getDriverId())).orElse(false);
        }
        return isArrivalStaff(user) && user.belongsToSchool(trip.getSchoolId());
    }

    private Principal toPrincipal(Jwt jwt) {
        Role role = Role.valueOf(jwt.getClaimAsString("role"));
        Long schoolId = jwt.hasClaim("schoolId") ? ((Number) jwt.getClaim("schoolId")).longValue() : null;
        CurrentUser currentUser = new CurrentUser(
                Long.valueOf(jwt.getSubject()), jwt.getClaimAsString("email"), role, schoolId);
        return new StompPrincipal(currentUser);
    }

    /** Principal name must be stable/unique per session for Spring's user-destination routing (/user/queue/...). */
    public record StompPrincipal(CurrentUser currentUser) implements Principal {
        @Override
        public String getName() {
            return currentUser.userId().toString();
        }
    }
}

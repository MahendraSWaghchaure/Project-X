package com.projectx.tracking.ws;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.projectx.people.domain.Child;
import com.projectx.people.domain.DriverProfile;
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
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.Message;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;

/** Pure unit tests of the STOMP channel interceptor; the end-to-end behaviour over a real socket is in the app module's SchoolAttendanceStompTest. */
class WebSocketAuthChannelInterceptorTest {

    private final JwtDecoder jwtDecoder = mock(JwtDecoder.class);
    private final TripRepository tripRepository = mock(TripRepository.class);
    private final DriverProfileRepository driverRepository = mock(DriverProfileRepository.class);
    private final VehicleRepository vehicleRepository = mock(VehicleRepository.class);
    private final RouteRepository routeRepository = mock(RouteRepository.class);
    private final ChildRepository childRepository = mock(ChildRepository.class);
    private final ParentProfileRepository parentProfileRepository = mock(ParentProfileRepository.class);
    private final WebSocketAuthChannelInterceptor interceptor = new WebSocketAuthChannelInterceptor(
            jwtDecoder, tripRepository, driverRepository, vehicleRepository, routeRepository, childRepository,
            parentProfileRepository);

    @BeforeEach
    void stubs() {
        when(jwtDecoder.decode("tok")).thenReturn(jwt(5L, "PARENT", null));
    }

    private static Jwt jwt(long sub, String role, Long schoolId) {
        Jwt.Builder b = Jwt.withTokenValue("tok").header("alg", "none").subject(String.valueOf(sub))
                .claim("role", role).claim("email", "u" + sub + "@x");
        if (schoolId != null) {
            b.claim("schoolId", schoolId);
        }
        return b.build();
    }

    /** A message as StompSubProtocolHandler hands it to the interceptor chain: mutable headers, accessor attached. */
    private static Message<byte[]> frame(StompCommand command, String destination, Principal user, String auth) {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(command);
        accessor.setLeaveMutable(true);
        accessor.setSessionId("s1");
        if (destination != null) {
            accessor.setDestination(destination);
            accessor.setSubscriptionId("sub-0");
        }
        if (auth != null) {
            accessor.addNativeHeader("Authorization", auth);
        }
        if (user != null) {
            accessor.setUser(user);
        }
        return MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
    }

    private static Principal principal(long userId, String role, Long schoolId) {
        return new WebSocketAuthChannelInterceptor.StompPrincipal(
                new com.projectx.common.security.CurrentUser(userId, "u@x", com.projectx.common.security.Role.valueOf(role), schoolId));
    }

    @Test
    void connectPersistsTheUserOnTheMessageItselfNotOnACopy() {
        Message<?> out = interceptor.preSend(frame(StompCommand.CONNECT, null, null, "Bearer tok"), null);
        // Regression: StompHeaderAccessor.wrap(message).setUser(..) wrote to a copy, so this was null and every later SUBSCRIBE was refused.
        Principal user = SimpMessageHeaderAccessor.getUser(out.getHeaders());
        assertThat(user).isInstanceOf(WebSocketAuthChannelInterceptor.StompPrincipal.class);
        assertThat(user.getName()).isEqualTo("5");
    }

    @Test
    void rootCauseDemo_wrapSetUserLandsOnACopy() {
        Message<byte[]> msg = frame(StompCommand.CONNECT, null, null, "Bearer tok");
        StompHeaderAccessor.wrap(msg).setUser(principal(5, "PARENT", null));
        assertThat(SimpMessageHeaderAccessor.getUser(msg.getHeaders())).as("wrap() => user lost").isNull();
    }

    @Test
    void connectWithoutBearerIsRejected() {
        assertThatThrownBy(() -> interceptor.preSend(frame(StompCommand.CONNECT, null, null, null), null)).isNotNull();
    }

    @Test
    void ownParentTopicAllowedOthersDenied() {
        Principal me = principal(5, "PARENT", null);
        interceptor.preSend(frame(StompCommand.SUBSCRIBE, "/topic/parent/5/arrivals", me, null), null);
        assertThatThrownBy(() -> interceptor.preSend(frame(StompCommand.SUBSCRIBE, "/topic/parent/6/arrivals", me, null), null))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void arrivalTopicsWithoutAuthenticatedUserAreDenied() {
        assertThatThrownBy(() -> interceptor.preSend(frame(StompCommand.SUBSCRIBE, "/topic/school/1/arrivals", null, null), null))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void wildcardsAndMalformedShapesAreDeniedForEveryone() {
        Principal platform = principal(1, "PLATFORM_ADMIN", null);
        for (String bad : new String[] {"/topic/**", "/topic/school/*/arrivals", "/topic/trip/*/arrivals", "/topic/parent/*/arrivals",
                "/topic/school/{id}/arrivals", "/topic/school/1/arrivals?", "/topic/school/**", "/topic/school/1/**",
                "/topic/school/1/arrivals/", "/topic/school/1/arrivals/x", "/topic/school/01/arrivals", "/topic/school/-1/arrivals",
                "/topic/school//arrivals", "/topic/trip/1/arrivals ", "/topic/vehicles/*/location", "/topic/trips/**",
                "/topic/school/1/arrivals\\"}) {
            assertThatThrownBy(() -> interceptor.preSend(frame(StompCommand.SUBSCRIBE, bad, platform, null), null))
                    .as(bad).isInstanceOf(AccessDeniedException.class);
        }
    }

    @Test
    void schoolTopicIsForStaffOfThatSchoolOnly() {
        interceptor.preSend(frame(StompCommand.SUBSCRIBE, "/topic/school/9/arrivals", principal(2, "SCHOOL_ADMIN", 9L), null), null);
        interceptor.preSend(frame(StompCommand.SUBSCRIBE, "/topic/school/9/arrivals", principal(2, "PLATFORM_ADMIN", null), null), null);
        for (Principal denied : new Principal[] {principal(2, "SCHOOL_ADMIN", 8L), principal(2, "PARENT", null), principal(2, "DRIVER", 9L),
                principal(2, "TEACHER", 9L)}) {
            assertThatThrownBy(() -> interceptor.preSend(frame(StompCommand.SUBSCRIBE, "/topic/school/9/arrivals", denied, null), null))
                    .isInstanceOf(AccessDeniedException.class);
        }
    }

    @Test
    void tripTopicIsForTheAssignedDriverOrStaffOfTheTripsSchool() {
        Trip trip = mock(Trip.class);
        when(trip.getSchoolId()).thenReturn(9L);
        when(trip.getDriverId()).thenReturn(77L);
        when(tripRepository.findById(anyLong())).thenReturn(Optional.empty());
        when(tripRepository.findById(3L)).thenReturn(Optional.of(trip));
        DriverProfile assigned = mock(DriverProfile.class);
        when(assigned.getId()).thenReturn(77L);
        DriverProfile other = mock(DriverProfile.class);
        when(other.getId()).thenReturn(78L);
        when(driverRepository.findByUserId(10L)).thenReturn(Optional.of(assigned));
        when(driverRepository.findByUserId(11L)).thenReturn(Optional.of(other));

        interceptor.preSend(frame(StompCommand.SUBSCRIBE, "/topic/trip/3/arrivals", principal(10, "DRIVER", 9L), null), null);
        interceptor.preSend(frame(StompCommand.SUBSCRIBE, "/topic/trip/3/arrivals", principal(2, "SCHOOL_ADMIN", 9L), null), null);
        for (Principal denied : new Principal[] {principal(11, "DRIVER", 9L), principal(12, "DRIVER", 9L),
                principal(2, "SCHOOL_ADMIN", 8L), principal(5, "PARENT", null), principal(2, "TEACHER", 9L)}) {
            assertThatThrownBy(() -> interceptor.preSend(frame(StompCommand.SUBSCRIBE, "/topic/trip/3/arrivals", denied, null), null))
                    .isInstanceOf(AccessDeniedException.class);
        }
        // Unknown trip: denied rather than leaking existence.
        assertThatThrownBy(() -> interceptor.preSend(
                frame(StompCommand.SUBSCRIBE, "/topic/trip/404/arrivals", principal(2, "SCHOOL_ADMIN", 9L), null), null))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void teacherAlertTopicIsExactMatchAndOwnerOnly() {
        Principal teacher = principal(21, "TEACHER", 9L);
        interceptor.preSend(frame(StompCommand.SUBSCRIBE, "/topic/teacher/21/alerts", teacher, null), null);
        // another teacher's alerts, and every other role (admins included), are denied
        for (Principal denied : new Principal[] {principal(22, "TEACHER", 9L), principal(21, "SCHOOL_ADMIN", 9L),
                principal(21, "PLATFORM_ADMIN", null), principal(21, "PARENT", null), principal(21, "DRIVER", 9L)}) {
            assertThatThrownBy(() -> interceptor.preSend(frame(StompCommand.SUBSCRIBE, "/topic/teacher/21/alerts", denied, null), null))
                    .as(denied.getName()).isInstanceOf(AccessDeniedException.class);
        }
        assertThatThrownBy(() -> interceptor.preSend(frame(StompCommand.SUBSCRIBE, "/topic/teacher/21/alerts", null, null), null))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void teacherNamespaceIsDenyByDefaultForMalformedShapes() {
        Principal teacher = principal(21, "TEACHER", 9L);
        for (String bad : new String[] {"/topic/teacher/", "/topic/teacher/21", "/topic/teacher/21/", "/topic/teacher/21/arrivals",
                "/topic/teacher/21/alerts/x", "/topic/teacher/021/alerts", "/topic/teacher/-1/alerts", "/topic/teacher/*/alerts",
                "/topic/teacher/**", "/topic/teacher/21/alerts?", "/topic/teacher/21/alerts ", "/topic/teacher/x/alerts",
                "/topic/teacher/21/alerts\\"}) {
            assertThatThrownBy(() -> interceptor.preSend(frame(StompCommand.SUBSCRIBE, bad, teacher, null), null))
                    .as(bad).isInstanceOf(AccessDeniedException.class);
        }
    }

    /**
     * Regression for a real vulnerability: these topics used to accept ANY authenticated session for
     * ANY vehicle/trip/school/user id (a parent could watch any other family's bus). Now mirrors the
     * exact rules the equivalent REST endpoints enforce (LiveLocationQueryService, GrievanceController).
     */
    @Test
    void vehicleLocationTopicMirrorsRestVehicleLocationRules() {
        Vehicle vehicle = mock(Vehicle.class);
        when(vehicle.getSchoolId()).thenReturn(9L);
        when(vehicleRepository.findById(anyLong())).thenReturn(Optional.empty());
        when(vehicleRepository.findById(12L)).thenReturn(Optional.of(vehicle));

        interceptor.preSend(frame(StompCommand.SUBSCRIBE, "/topic/vehicles/12/location", principal(1, "PLATFORM_ADMIN", null), null), null);
        interceptor.preSend(frame(StompCommand.SUBSCRIBE, "/topic/vehicles/12/location", principal(2, "SCHOOL_ADMIN", 9L), null), null);
        interceptor.preSend(frame(StompCommand.SUBSCRIBE, "/topic/vehicles/12/location", principal(3, "DRIVER", 9L), null), null);
        for (Principal denied : new Principal[] {principal(2, "SCHOOL_ADMIN", 8L), principal(3, "DRIVER", 8L), principal(5, "PARENT", null)}) {
            assertThatThrownBy(() -> interceptor.preSend(frame(StompCommand.SUBSCRIBE, "/topic/vehicles/12/location", denied, null), null))
                    .isInstanceOf(AccessDeniedException.class);
        }
        // Unknown vehicle: denied rather than leaking existence.
        assertThatThrownBy(() -> interceptor.preSend(
                frame(StompCommand.SUBSCRIBE, "/topic/vehicles/404/location", principal(2, "SCHOOL_ADMIN", 9L), null), null))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void vehicleLocationTopicAllowsAParentWhoseChildRidesThatVehicleOnly() {
        Vehicle vehicle = mock(Vehicle.class);
        when(vehicle.getSchoolId()).thenReturn(9L);
        when(vehicleRepository.findById(12L)).thenReturn(Optional.of(vehicle));
        Route route = mock(Route.class);
        when(route.getVehicleId()).thenReturn(12L);
        when(routeRepository.findById(44L)).thenReturn(Optional.of(route));
        Child myChild = mock(Child.class);
        when(myChild.getRouteId()).thenReturn(44L);
        ParentProfile me = mock(ParentProfile.class);
        when(me.getId()).thenReturn(100L);
        when(parentProfileRepository.findByUserId(5L)).thenReturn(Optional.of(me));
        when(childRepository.findByGuardianParentId(100L)).thenReturn(List.of(myChild));
        ParentProfile other = mock(ParentProfile.class);
        when(other.getId()).thenReturn(101L);
        when(parentProfileRepository.findByUserId(6L)).thenReturn(Optional.of(other));
        when(childRepository.findByGuardianParentId(101L)).thenReturn(List.of());

        interceptor.preSend(frame(StompCommand.SUBSCRIBE, "/topic/vehicles/12/location", principal(5, "PARENT", null), null), null);
        assertThatThrownBy(() -> interceptor.preSend(
                frame(StompCommand.SUBSCRIBE, "/topic/vehicles/12/location", principal(6, "PARENT", null), null), null))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void tripLiveDataTopicsMirrorTripArrivalRulesPlusTheChildsParent() {
        Trip trip = mock(Trip.class);
        when(trip.getSchoolId()).thenReturn(9L);
        when(trip.getDriverId()).thenReturn(77L);
        when(trip.getRouteId()).thenReturn(44L);
        when(tripRepository.findById(anyLong())).thenReturn(Optional.empty());
        when(tripRepository.findById(3L)).thenReturn(Optional.of(trip));
        DriverProfile assigned = mock(DriverProfile.class);
        when(assigned.getId()).thenReturn(77L);
        when(driverRepository.findByUserId(10L)).thenReturn(Optional.of(assigned));
        Child myChild = mock(Child.class);
        when(myChild.getRouteId()).thenReturn(44L);
        ParentProfile me = mock(ParentProfile.class);
        when(me.getId()).thenReturn(100L);
        when(parentProfileRepository.findByUserId(5L)).thenReturn(Optional.of(me));
        when(childRepository.findByGuardianParentId(100L)).thenReturn(List.of(myChild));

        for (String destination : new String[] {"/topic/trips/3/location", "/topic/trips/3/sos"}) {
            interceptor.preSend(frame(StompCommand.SUBSCRIBE, destination, principal(10, "DRIVER", 9L), null), null);
            interceptor.preSend(frame(StompCommand.SUBSCRIBE, destination, principal(2, "SCHOOL_ADMIN", 9L), null), null);
            interceptor.preSend(frame(StompCommand.SUBSCRIBE, destination, principal(5, "PARENT", null), null), null);
            for (Principal denied : new Principal[] {principal(11, "DRIVER", 9L), principal(2, "SCHOOL_ADMIN", 8L), principal(6, "PARENT", null)}) {
                assertThatThrownBy(() -> interceptor.preSend(frame(StompCommand.SUBSCRIBE, destination, denied, null), null))
                        .as("%s / %s".formatted(destination, denied.getName())).isInstanceOf(AccessDeniedException.class);
            }
        }
    }

    @Test
    void userGrievanceTopicIsOwnerOnlyRegardlessOfRole() {
        interceptor.preSend(frame(StompCommand.SUBSCRIBE, "/topic/users/5/grievances", principal(5, "PARENT", null), null), null);
        interceptor.preSend(frame(StompCommand.SUBSCRIBE, "/topic/users/7/grievances", principal(7, "DRIVER", 9L), null), null);
        assertThatThrownBy(() -> interceptor.preSend(
                frame(StompCommand.SUBSCRIBE, "/topic/users/5/grievances", principal(6, "PARENT", null), null), null))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void schoolGrievanceTopicIsStaffOfThatSchoolOrPlatformAdmin() {
        interceptor.preSend(frame(StompCommand.SUBSCRIBE, "/topic/schools/9/grievances", principal(2, "SCHOOL_ADMIN", 9L), null), null);
        interceptor.preSend(frame(StompCommand.SUBSCRIBE, "/topic/schools/9/grievances", principal(1, "PLATFORM_ADMIN", null), null), null);
        for (Principal denied : new Principal[] {principal(2, "SCHOOL_ADMIN", 8L), principal(5, "PARENT", null), principal(3, "DRIVER", 9L)}) {
            assertThatThrownBy(() -> interceptor.preSend(frame(StompCommand.SUBSCRIBE, "/topic/schools/9/grievances", denied, null), null))
                    .isInstanceOf(AccessDeniedException.class);
        }
    }

    @Test
    void platformAdminGrievanceTopicIsPlatformAdminOnly() {
        interceptor.preSend(frame(StompCommand.SUBSCRIBE, "/topic/platform-admin/grievances", principal(1, "PLATFORM_ADMIN", null), null), null);
        for (Principal denied : new Principal[] {principal(2, "SCHOOL_ADMIN", 9L), principal(5, "PARENT", null)}) {
            assertThatThrownBy(() -> interceptor.preSend(frame(StompCommand.SUBSCRIBE, "/topic/platform-admin/grievances", denied, null), null))
                    .isInstanceOf(AccessDeniedException.class);
        }
    }
}

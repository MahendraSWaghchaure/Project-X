package com.projectx.grievance.service;

import com.projectx.common.events.AuditEvent;
import com.projectx.common.exception.ForbiddenOperationException;
import com.projectx.common.exception.ResourceNotFoundException;
import com.projectx.common.security.CurrentUser;
import com.projectx.common.security.CurrentUserContext;
import com.projectx.common.security.Role;
import com.projectx.common.web.PageResponse;
import com.projectx.grievance.domain.Grievance;
import com.projectx.grievance.domain.GrievanceEvent;
import com.projectx.grievance.domain.GrievanceStatus;
import com.projectx.grievance.domain.GrievanceType;
import com.projectx.grievance.dto.CreateGrievanceRequest;
import com.projectx.grievance.dto.GrievanceResponse;
import com.projectx.grievance.dto.GrievanceResponse.TimelineEntry;
import com.projectx.grievance.dto.UpdateGrievanceStatusRequest;
import com.projectx.grievance.repo.GrievanceEventRepository;
import com.projectx.grievance.repo.GrievanceRepository;
import com.projectx.grievance.ws.GrievanceBroadcaster;
import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class GrievanceService {

    /** The un-paged list endpoints return the newest N; use the paged search for anything older. */
    static final int MAX_LIST = 200;
    static final int MAX_PAGE_SIZE = 100;

    private final GrievanceRepository grievanceRepository;
    private final GrievanceEventRepository eventRepository;
    private final GrievanceBroadcaster broadcaster;
    private final ObjectProvider<GrievanceDirectory> directoryProvider;
    private final ApplicationEventPublisher eventPublisher;

    public GrievanceService(GrievanceRepository grievanceRepository, GrievanceEventRepository eventRepository,
                            GrievanceBroadcaster broadcaster, ObjectProvider<GrievanceDirectory> directoryProvider,
                            ApplicationEventPublisher eventPublisher) {
        this.grievanceRepository = grievanceRepository;
        this.eventRepository = eventRepository;
        this.broadcaster = broadcaster;
        this.directoryProvider = directoryProvider;
        this.eventPublisher = eventPublisher;
    }

    /** Any of the three roles that live a trip day-to-day (parent, driver, school admin) can raise one. */
    public GrievanceResponse raise(CreateGrievanceRequest request) {
        CurrentUser user = CurrentUserContext.get();
        if (user.role() == Role.PLATFORM_ADMIN) {
            throw new ForbiddenOperationException("Platform admin resolves grievances, not raise them");
        }
        Long schoolId = request.schoolId() != null ? request.schoolId() : user.schoolId();
        // SCHOOL_ADMIN/DRIVER tokens carry a schoolId claim and must raise about their own school;
        // PARENT tokens never carry one (guardianship-scoped, not school-scoped - see CurrentUser's
        // javadoc), so there's nothing to check them against here and their claimed schoolId is
        // trusted at face value, same as CreateChildRequest.primaryParentId elsewhere.
        if (schoolId != null && user.role() != Role.PARENT) {
            CurrentUserContext.requireSchoolAccess(schoolId);
        }

        GrievanceType type = resolveType(request, user.role());
        String other = type == GrievanceType.OTHER ? blankToNull(request.otherDescription()) : null;
        if (type == GrievanceType.OTHER && other == null && request.type() != null) {
            throw new IllegalArgumentException("otherDescription is required when the type is OTHER");
        }
        GrievanceDirectory directory = directoryProvider.getIfAvailable();
        if (directory != null) {
            directory.validateLinks(user.role(), user.userId(), schoolId, request.vehicleId(), request.driverId(),
                    request.childId(), request.tripId());
        }

        String subject = blankToNull(request.subject()) != null ? request.subject().trim() : humanize(type);
        Grievance grievance = new Grievance(schoolId, user.userId(), user.role(), user.email(),
                type.legacyCategory(), subject, request.description().trim());
        if (request.category() != null && request.type() == null) {
            // Legacy client: keep exactly the category it sent.
            grievance = new Grievance(schoolId, user.userId(), user.role(), user.email(), request.category(), subject,
                    request.description().trim());
        }
        grievance.classify(type, other);
        grievance.linkTo(request.vehicleId(), request.driverId(), request.childId(), request.tripId());
        grievance = grievanceRepository.save(grievance);
        eventRepository.save(new GrievanceEvent(grievance.getId(), GrievanceStatus.OPEN, null, user.userId()));
        eventPublisher.publishEvent(AuditEvent.of("GRIEVANCE_RAISED", "GRIEVANCE", grievance.getId(), schoolId,
                "type=" + type + " by " + user.role()));
        GrievanceResponse response = enrichOne(grievance, null);
        broadcaster.broadcast(response);
        return response;
    }

    @Transactional(readOnly = true)
    public List<GrievanceResponse> listMine() {
        Long userId = CurrentUserContext.get().userId();
        return enrich(grievanceRepository.findByRaisedByUserIdOrderByCreatedAtDesc(userId, PageRequest.of(0, MAX_LIST)));
    }

    /** Newest 200 grievances of one school (enriched). The paged, filterable variant is {@link #search}. */
    @Transactional(readOnly = true)
    public List<GrievanceResponse> listForSchool(Long schoolId) {
        CurrentUserContext.requireSchoolAccess(schoolId);
        return enrich(grievanceRepository.findBySchoolIdOrderByCreatedAtDesc(schoolId, PageRequest.of(0, MAX_LIST)));
    }

    /** Platform-wide view - across every school, including grievances raised with no school context (newest 200). */
    @Transactional(readOnly = true)
    public List<GrievanceResponse> listAll() {
        return enrich(grievanceRepository.findAllByOrderByCreatedAtDesc(PageRequest.of(0, MAX_LIST)));
    }

    /**
     * Paged, filterable table. {@code schoolId} null = every school (platform admin only); a school admin is always
     * pinned to their own school. Size capped at 100.
     */
    @Transactional(readOnly = true)
    public PageResponse<GrievanceResponse> search(Long schoolId, GrievanceStatus status, GrievanceType type, Integer page, Integer size) {
        CurrentUser user = CurrentUserContext.get();
        if (schoolId == null && !user.isPlatformAdmin()) {
            throw new ForbiddenOperationException("Not authorized to list every school's grievances");
        }
        if (schoolId != null) {
            CurrentUserContext.requireSchoolAccess(schoolId);
        }
        Specification<Grievance> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (schoolId != null) {
                predicates.add(cb.equal(root.get("schoolId"), schoolId));
            }
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            if (type != null) {
                predicates.add(cb.equal(root.get("type"), type));
            }
            return cb.and(predicates.toArray(Predicate[]::new));
        };
        var pageable = PageRequest.of(Math.max(page == null ? 0 : page, 0), Math.min(Math.max(size == null ? 20 : size, 1), MAX_PAGE_SIZE),
                Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id")));
        var result = grievanceRepository.findAll(spec, pageable);
        List<GrievanceResponse> content = enrich(result.getContent());
        return new PageResponse<>(content, result.getNumber(), result.getSize(), result.getTotalElements(),
                result.getTotalPages(), result.isLast());
    }

    /** Full record with the status timeline. Platform admin, the grievance's school admin, or the person who raised it. */
    @Transactional(readOnly = true)
    public GrievanceResponse detail(Long grievanceId) {
        CurrentUser user = CurrentUserContext.get();
        Grievance grievance = grievanceRepository.findById(grievanceId)
                .orElseThrow(() -> ResourceNotFoundException.of("Grievance", grievanceId));
        boolean raiser = Objects.equals(grievance.getRaisedByUserId(), user.userId());
        boolean staff = (user.role() == Role.PLATFORM_ADMIN || user.role() == Role.SCHOOL_ADMIN)
                && user.belongsToSchool(grievance.getSchoolId());
        if (!raiser && !staff) {
            // Not found rather than forbidden: never confirm that someone else's grievance id exists.
            throw ResourceNotFoundException.of("Grievance", grievanceId);
        }
        return enrichOne(grievance, timeline(grievanceId));
    }

    public GrievanceResponse updateStatus(Long grievanceId, UpdateGrievanceStatusRequest request) {
        CurrentUser user = CurrentUserContext.get();
        Grievance grievance = grievanceRepository.findById(grievanceId)
                .orElseThrow(() -> ResourceNotFoundException.of("Grievance", grievanceId));
        if (!user.belongsToSchool(grievance.getSchoolId())) {
            throw new ForbiddenOperationException("Not authorized to resolve this grievance");
        }

        grievance.resolve(request.status(), request.resolutionNote(), user.userId());
        eventRepository.save(new GrievanceEvent(grievanceId, request.status(), blankToNull(request.resolutionNote()), user.userId()));
        eventPublisher.publishEvent(AuditEvent.of("GRIEVANCE_STATUS_CHANGED", "GRIEVANCE", grievanceId, grievance.getSchoolId(),
                "status=" + request.status()));
        GrievanceResponse response = enrichOne(grievance, timeline(grievanceId));
        broadcaster.broadcast(response);
        return response;
    }

    // ------------------------------------------------------------------ helpers

    private List<TimelineEntry> timeline(Long grievanceId) {
        return eventRepository.findByGrievanceIdOrderByCreatedAtAscIdAsc(grievanceId).stream().map(TimelineEntry::from).toList();
    }

    private GrievanceType resolveType(CreateGrievanceRequest request, Role role) {
        if (request.type() != null) {
            if (!GrievanceType.allowedFor(role).contains(request.type())) {
                throw new IllegalArgumentException("Complaint type " + request.type() + " is not available for your role");
            }
            return request.type();
        }
        if (request.category() == null) {
            throw new IllegalArgumentException("type is required");
        }
        return GrievanceType.fromLegacy(request.category());
    }

    private GrievanceResponse enrichOne(Grievance g, List<TimelineEntry> timeline) {
        return enrich(List.of(g), timeline).get(0);
    }

    private List<GrievanceResponse> enrich(List<Grievance> grievances) {
        return enrich(grievances, null);
    }

    private List<GrievanceResponse> enrich(List<Grievance> grievances, List<TimelineEntry> timeline) {
        if (grievances.isEmpty()) {
            return List.of();
        }
        GrievanceDirectory directory = directoryProvider.getIfAvailable();
        if (directory == null) {
            return grievances.stream().map(g -> GrievanceResponse.from(g, null, null, null, null, null, timeline)).toList();
        }
        Map<Long, String> users = directory.userNames(ids(grievances, Grievance::getRaisedByUserId));
        Map<Long, String> drivers = directory.driverNames(ids(grievances, Grievance::getDriverId));
        Map<Long, String> vehicles = directory.vehicleNumbers(ids(grievances, Grievance::getVehicleId));
        Map<Long, String> children = directory.childNames(ids(grievances, Grievance::getChildId));
        Map<Long, String> schools = directory.schoolNames(ids(grievances, Grievance::getSchoolId));
        return grievances.stream().map(g -> GrievanceResponse.from(g,
                users.getOrDefault(g.getRaisedByUserId(), g.getRaisedByEmail()),
                g.getDriverId() == null ? null : drivers.get(g.getDriverId()),
                g.getVehicleId() == null ? null : vehicles.get(g.getVehicleId()),
                g.getChildId() == null ? null : children.get(g.getChildId()),
                g.getSchoolId() == null ? null : schools.get(g.getSchoolId()),
                timeline)).toList();
    }

    private static Set<Long> ids(Collection<Grievance> grievances, java.util.function.Function<Grievance, Long> extractor) {
        Set<Long> out = new LinkedHashSet<>();
        for (Grievance g : grievances) {
            Long id = extractor.apply(g);
            if (id != null) {
                out.add(id);
            }
        }
        return out;
    }

    private static String humanize(GrievanceType type) {
        String lower = type.name().toLowerCase(Locale.ROOT).replace('_', ' ');
        return Character.toUpperCase(lower.charAt(0)) + lower.substring(1);
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }
}

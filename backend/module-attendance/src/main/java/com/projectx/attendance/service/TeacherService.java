package com.projectx.attendance.service;

import com.projectx.attendance.domain.Classroom;
import com.projectx.attendance.domain.TeacherProfile;
import com.projectx.attendance.dto.StaffDtos.InviteTeacherRequest;
import com.projectx.attendance.dto.StaffDtos.InviteTeacherResponse;
import com.projectx.attendance.dto.StaffDtos.TeacherResponse;
import com.projectx.attendance.dto.StaffDtos.UpdateTeacherRequest;
import com.projectx.attendance.repo.ClassroomRepository;
import com.projectx.attendance.repo.TeacherProfileRepository;
import com.projectx.attendance.repo.TimetablePeriodRepository;
import com.projectx.common.events.AuditEvent;
import com.projectx.common.exception.ConflictException;
import com.projectx.common.exception.ResourceNotFoundException;
import com.projectx.common.security.CurrentUserContext;
import com.projectx.common.security.Role;
import com.projectx.identity.domain.UserAccount;
import com.projectx.identity.domain.UserStatus;
import com.projectx.identity.repo.UserAccountRepository;
import com.projectx.identity.service.InvitationService;
import com.projectx.identity.service.UserProvisioningService;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Teacher onboarding reuses the parent-invite pattern: INVITED account bound to the school, set password via
 * /auth/invitations/{token}/accept. On top of the login there is a {@link TeacherProfile} (name, phone, qualification), an
 * optional class-teacher assignment, and the invite status the table shows (INVITED / EXPIRED / ACTIVE). Everything shown
 * in the table for a whole page is fetched in a fixed number of batched queries.
 */
@Service
@Transactional
public class TeacherService {

    private static final int MAX_TEACHERS_LISTED = 200;

    private final UserProvisioningService userProvisioningService;
    private final InvitationService invitationService;
    private final UserAccountRepository userAccountRepository;
    private final TeacherProfileRepository profileRepository;
    private final ClassroomRepository classroomRepository;
    private final TimetablePeriodRepository periodRepository;
    private final ClassroomService classroomService;
    private final ApplicationEventPublisher eventPublisher;

    public TeacherService(UserProvisioningService userProvisioningService, InvitationService invitationService,
                          UserAccountRepository userAccountRepository, TeacherProfileRepository profileRepository,
                          ClassroomRepository classroomRepository, TimetablePeriodRepository periodRepository,
                          ClassroomService classroomService, ApplicationEventPublisher eventPublisher) {
        this.userProvisioningService = userProvisioningService;
        this.invitationService = invitationService;
        this.userAccountRepository = userAccountRepository;
        this.profileRepository = profileRepository;
        this.classroomRepository = classroomRepository;
        this.periodRepository = periodRepository;
        this.classroomService = classroomService;
        this.eventPublisher = eventPublisher;
    }

    public InviteTeacherResponse invite(Long schoolId, InviteTeacherRequest request) {
        CurrentUserContext.requireSchoolAccess(schoolId);
        Classroom classroom = null;
        if (request.classTeacherOfClassroomId() != null) {
            classroom = classroomRepository.findByIdAndSchoolId(request.classTeacherOfClassroomId(), schoolId)
                    .orElseThrow(() -> ResourceNotFoundException.of("Classroom", request.classTeacherOfClassroomId()));
            requireNoClassTeacher(classroom, null);
        }
        UserAccount user = userProvisioningService.provisionInvited(request.email().trim(), blankToNull(request.phone()), Role.TEACHER, schoolId);
        String[] name = splitName(request.name(), user.getEmail());
        profileRepository.save(new TeacherProfile(user.getId(), schoolId, name[0], name[1], blankToNull(request.phone()),
                blankToNull(request.qualification())));
        if (classroom != null) {
            classroomService.applyClassTeacher(schoolId, classroom, user.getId());
        }
        String token = invitationService.issueInvitation(user.getId());
        eventPublisher.publishEvent(AuditEvent.of("TEACHER_INVITED", "USER", user.getId(), schoolId, "Teacher " + user.getEmail() + " invited"));
        return new InviteTeacherResponse(user.getId(), user.getEmail(), token);
    }

    @Transactional(readOnly = true)
    public List<TeacherResponse> list(Long schoolId) {
        CurrentUserContext.requireSchoolAccess(schoolId);
        List<UserAccount> users = userAccountRepository
                .findBySchoolIdAndRole(schoolId, Role.TEACHER, PageRequest.of(0, MAX_TEACHERS_LISTED, Sort.by("email"))).getContent();
        return toResponses(users);
    }

    @Transactional(readOnly = true)
    public TeacherResponse get(Long schoolId, Long teacherId) {
        CurrentUserContext.requireSchoolAccess(schoolId);
        return toResponses(List.of(requireTeacher(schoolId, teacherId))).get(0);
    }

    /** Full replace of name / phone / qualification and of the class-teacher assignment (null = not a class teacher). */
    public TeacherResponse update(Long schoolId, Long teacherId, UpdateTeacherRequest request) {
        CurrentUserContext.requireSchoolAccess(schoolId);
        UserAccount user = requireTeacher(schoolId, teacherId);
        String[] name = splitName(request.name(), user.getEmail());
        TeacherProfile profile = profileRepository.findByUserId(teacherId)
                .orElseGet(() -> new TeacherProfile(teacherId, schoolId, name[0], name[1], null, null));
        profile.update(name[0], name[1], blankToNull(request.phone()), blankToNull(request.qualification()));
        profileRepository.save(profile);
        user.setPhone(blankToNull(request.phone()));

        Classroom current = classroomRepository.findByClassTeacherId(teacherId).orElse(null);
        Long wanted = request.classTeacherOfClassroomId();
        if (wanted == null) {
            if (current != null) {
                classroomService.applyClassTeacher(schoolId, current, null);
            }
        } else if (current == null || !current.getId().equals(wanted)) {
            Classroom target = classroomRepository.findByIdAndSchoolId(wanted, schoolId)
                    .orElseThrow(() -> ResourceNotFoundException.of("Classroom", wanted));
            requireNoClassTeacher(target, teacherId);
            if (current != null) {
                classroomService.applyClassTeacher(schoolId, current, null);
            }
            classroomService.applyClassTeacher(schoolId, target, teacherId);
        }
        eventPublisher.publishEvent(AuditEvent.of("TEACHER_UPDATED", "USER", teacherId, schoolId, "Teacher profile updated"));
        return toResponses(List.of(user)).get(0);
    }

    /** Re-sends an invitation that is still pending or has expired: revokes the old link and issues a fresh 7-day one. */
    public InviteTeacherResponse resendInvite(Long schoolId, Long teacherId) {
        CurrentUserContext.requireSchoolAccess(schoolId);
        UserAccount user = requireTeacher(schoolId, teacherId);
        if (user.getStatus() != UserStatus.INVITED) {
            throw new ConflictException("This teacher has already accepted the invitation");
        }
        String token = invitationService.reissueInvitation(user.getId());
        eventPublisher.publishEvent(AuditEvent.of("TEACHER_INVITE_RESENT", "USER", teacherId, schoolId, "Invitation re-issued for " + user.getEmail()));
        return new InviteTeacherResponse(user.getId(), user.getEmail(), token);
    }

    // ------------------------------------------------------------------ helpers

    /** The teacher forms never silently displace a classroom's current class teacher (use PUT .../class-teacher to replace one on purpose). */
    private static void requireNoClassTeacher(Classroom classroom, Long allowedTeacherId) {
        if (classroom.getClassTeacherId() != null && !classroom.getClassTeacherId().equals(allowedTeacherId)) {
            throw new ConflictException(classroom.displayName() + " already has a class teacher - change it from the classroom instead");
        }
    }

    private UserAccount requireTeacher(Long schoolId, Long teacherId) {
        UserAccount user = userAccountRepository.findById(teacherId).orElse(null);
        if (user == null || user.getRole() != Role.TEACHER || !Objects.equals(user.getSchoolId(), schoolId)) {
            throw ResourceNotFoundException.of("Teacher", teacherId);
        }
        return user;
    }

    /** profile, invite expiry, class-teacher assignment and period count for the whole page: 4 queries total. */
    private List<TeacherResponse> toResponses(List<UserAccount> users) {
        if (users.isEmpty()) {
            return List.of();
        }
        List<Long> ids = users.stream().map(UserAccount::getId).toList();
        Map<Long, TeacherProfile> profiles = profileRepository.findByUserIdIn(ids).stream()
                .collect(Collectors.toMap(TeacherProfile::getUserId, p -> p));
        Map<Long, Instant> inviteExpiry = invitationService.latestOutstandingExpiry(ids);
        Map<Long, Classroom> classByTeacher = new HashMap<>();
        for (Classroom c : classroomRepository.findByClassTeacherIdIn(ids)) {
            classByTeacher.put(c.getClassTeacherId(), c);
        }
        Map<Long, Long> periods = new HashMap<>();
        for (Object[] row : periodRepository.countByTeacherIds(ids)) {
            periods.put((Long) row[0], ((Number) row[1]).longValue());
        }
        Instant now = Instant.now();
        return users.stream().map(u -> {
            TeacherProfile p = profiles.get(u.getId());
            Classroom c = classByTeacher.get(u.getId());
            Instant expiry = inviteExpiry.get(u.getId());
            String inviteStatus = switch (u.getStatus()) {
                case ACTIVE -> "ACTIVE";
                case DISABLED -> "DISABLED";
                case INVITED -> expiry != null && expiry.isAfter(now) ? "INVITED" : "EXPIRED";
            };
            String first = p != null ? p.getFirstName() : localPart(u.getEmail());
            String last = p != null ? p.getLastName() : null;
            return new TeacherResponse(u.getId(), u.getEmail(), p != null && p.getPhone() != null ? p.getPhone() : u.getPhone(),
                    u.getStatus().name(), p != null ? p.fullName() : first, first, last, p != null ? p.getQualification() : null,
                    inviteStatus, u.getStatus() == UserStatus.INVITED ? expiry : null,
                    c == null ? null : c.getId(), c == null ? null : c.displayName(), periods.getOrDefault(u.getId(), 0L));
        }).toList();
    }

    private static String[] splitName(String name, String email) {
        String n = blankToNull(name);
        if (n == null) {
            return new String[] {localPart(email), null};
        }
        int space = n.lastIndexOf(' ');
        return space < 0 ? new String[] {n, null} : new String[] {n.substring(0, space).trim(), n.substring(space + 1).trim()};
    }

    private static String localPart(String email) {
        int at = email.indexOf('@');
        return at > 0 ? email.substring(0, at) : email;
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }
}

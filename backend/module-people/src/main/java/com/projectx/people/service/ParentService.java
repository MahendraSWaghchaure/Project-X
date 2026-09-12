package com.projectx.people.service;

import com.projectx.common.events.AuditEvent;
import com.projectx.common.exception.ResourceNotFoundException;
import com.projectx.common.geo.GeoPoints;
import com.projectx.common.security.CurrentUserContext;
import com.projectx.common.security.Role;
import com.projectx.identity.domain.UserAccount;
import com.projectx.identity.domain.UserStatus;
import com.projectx.identity.repo.UserAccountRepository;
import com.projectx.identity.service.InvitationService;
import com.projectx.identity.service.UserProvisioningService;
import com.projectx.people.domain.ParentProfile;
import com.projectx.people.dto.CompleteParentProfileRequest;
import com.projectx.people.dto.InviteParentRequest;
import com.projectx.people.dto.InviteParentResponse;
import com.projectx.people.dto.ParentResponse;
import com.projectx.people.repo.ParentProfileRepository;
import java.util.List;
import org.locationtech.jts.geom.Point;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class ParentService {

    private final ParentProfileRepository parentProfileRepository;
    private final UserAccountRepository userAccountRepository;
    private final UserProvisioningService userProvisioningService;
    private final InvitationService invitationService;
    private final ApplicationEventPublisher eventPublisher;

    public ParentService(ParentProfileRepository parentProfileRepository, UserAccountRepository userAccountRepository,
                          UserProvisioningService userProvisioningService, InvitationService invitationService,
                          ApplicationEventPublisher eventPublisher) {
        this.parentProfileRepository = parentProfileRepository;
        this.userAccountRepository = userAccountRepository;
        this.userProvisioningService = userProvisioningService;
        this.invitationService = invitationService;
        this.eventPublisher = eventPublisher;
    }

    public InviteParentResponse inviteParent(Long schoolId, InviteParentRequest request) {
        CurrentUserContext.requireSchoolAccess(schoolId);
        return provisionParentInvite(schoolId, request);
    }

    /**
     * Same provisioning as inviteParent, without the school-admin access gate - for callers who've
     * already done their own authorization (e.g. ChildService.inviteGuardian checks the
     * caller is a guardian of the specific child, which is the correct check there, not
     * "is a school admin for this school").
     */
    public InviteParentResponse provisionParentInvite(Long schoolId, InviteParentRequest request) {
        UserAccount user = userProvisioningService.provisionInvited(request.email(), request.phone(), Role.PARENT, null);
        ParentProfile profile = parentProfileRepository.save(new ParentProfile(user.getId(), schoolId, null, null));
        String inviteToken = invitationService.issueInvitation(user.getId());

        return new InviteParentResponse(profile.getId(), user.getEmail(), inviteToken);
    }

    public ParentResponse completeMyProfile(CompleteParentProfileRequest request) {
        ParentProfile profile = findByUserIdOrThrow(CurrentUserContext.get().userId());
        Point homeLocation = (request.latitude() != null && request.longitude() != null)
                ? GeoPoints.toPoint(request.latitude(), request.longitude())
                : null;
        profile.completeProfile(request.firstName(), request.lastName(), request.phone(), request.address(), homeLocation);
        return toResponse(profile);
    }

    @Transactional(readOnly = true)
    public ParentResponse getMyProfile() {
        return toResponse(findByUserIdOrThrow(CurrentUserContext.get().userId()));
    }

    @Transactional(readOnly = true)
    public List<ParentResponse> listParentsForSchool(Long schoolId) {
        CurrentUserContext.requireSchoolAccess(schoolId);
        return parentProfileRepository.findByInvitedBySchoolId(schoolId).stream().map(this::toResponse).toList();
    }

    /** "Delete" from the school admin's perspective - disabling the login keeps the parent/child/trip history intact and is reversible. */
    public void deactivateParent(Long schoolId, Long parentProfileId) {
        CurrentUserContext.requireSchoolAccess(schoolId);
        ParentProfile parent = findWithinSchoolOrThrow(schoolId, parentProfileId);
        UserAccount account = userAccountRepository.findById(parent.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("No login for this parent"));
        account.setStatus(UserStatus.DISABLED);
        eventPublisher.publishEvent(AuditEvent.of("PARENT_DEACTIVATED", "PARENT_PROFILE", parent.getId(), schoolId,
                "Parent account " + account.getEmail() + " disabled"));
    }

    ParentProfile findByUserIdOrThrow(Long userId) {
        return parentProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Parent profile not found for current user"));
    }

    private ParentProfile findWithinSchoolOrThrow(Long schoolId, Long parentProfileId) {
        ParentProfile parent = parentProfileRepository.findById(parentProfileId)
                .orElseThrow(() -> ResourceNotFoundException.of("Parent", parentProfileId));
        if (!parent.getInvitedBySchoolId().equals(schoolId)) {
            throw ResourceNotFoundException.of("Parent", parentProfileId);
        }
        return parent;
    }

    private ParentResponse toResponse(ParentProfile parent) {
        UserAccount account = userAccountRepository.findById(parent.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("No login for this parent"));
        return ParentResponse.from(parent, account);
    }
}

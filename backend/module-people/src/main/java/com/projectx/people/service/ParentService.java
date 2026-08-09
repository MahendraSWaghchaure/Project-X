package com.projectx.people.service;

import com.projectx.common.exception.ResourceNotFoundException;
import com.projectx.common.geo.GeoPoints;
import com.projectx.common.security.CurrentUserContext;
import com.projectx.common.security.Role;
import com.projectx.identity.domain.UserAccount;
import com.projectx.identity.service.InvitationService;
import com.projectx.identity.service.UserProvisioningService;
import com.projectx.people.domain.ParentProfile;
import com.projectx.people.dto.CompleteParentProfileRequest;
import com.projectx.people.dto.InviteParentRequest;
import com.projectx.people.dto.InviteParentResponse;
import com.projectx.people.dto.ParentResponse;
import com.projectx.people.repo.ParentProfileRepository;
import org.locationtech.jts.geom.Point;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class ParentService {

    private final ParentProfileRepository parentProfileRepository;
    private final UserProvisioningService userProvisioningService;
    private final InvitationService invitationService;

    public ParentService(ParentProfileRepository parentProfileRepository, UserProvisioningService userProvisioningService,
                          InvitationService invitationService) {
        this.parentProfileRepository = parentProfileRepository;
        this.userProvisioningService = userProvisioningService;
        this.invitationService = invitationService;
    }

    public InviteParentResponse inviteParent(Long schoolId, InviteParentRequest request) {
        CurrentUserContext.requireSchoolAccess(schoolId);

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
        return ParentResponse.from(profile);
    }

    @Transactional(readOnly = true)
    public ParentResponse getMyProfile() {
        return ParentResponse.from(findByUserIdOrThrow(CurrentUserContext.get().userId()));
    }

    ParentProfile findByUserIdOrThrow(Long userId) {
        return parentProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Parent profile not found for current user"));
    }
}

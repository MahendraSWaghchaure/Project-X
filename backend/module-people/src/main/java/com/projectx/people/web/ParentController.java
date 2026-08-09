package com.projectx.people.web;

import com.projectx.common.web.ApiResponse;
import com.projectx.people.dto.CompleteParentProfileRequest;
import com.projectx.people.dto.InviteParentRequest;
import com.projectx.people.dto.InviteParentResponse;
import com.projectx.people.dto.ParentResponse;
import com.projectx.people.service.ParentService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ParentController {

    private final ParentService parentService;

    public ParentController(ParentService parentService) {
        this.parentService = parentService;
    }

    @PostMapping("/schools/{schoolId}/parents/invitations")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN', 'SCHOOL_ADMIN')")
    public ApiResponse<InviteParentResponse> inviteParent(@PathVariable Long schoolId,
                                                            @Valid @RequestBody InviteParentRequest request) {
        return ApiResponse.ok(parentService.inviteParent(schoolId, request));
    }

    @GetMapping("/schools/{schoolId}/parents")
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN', 'SCHOOL_ADMIN')")
    public ApiResponse<List<ParentResponse>> listParentsForSchool(@PathVariable Long schoolId) {
        return ApiResponse.ok(parentService.listParentsForSchool(schoolId));
    }

    @DeleteMapping("/schools/{schoolId}/parents/{parentId}")
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN', 'SCHOOL_ADMIN')")
    public ApiResponse<Void> deactivateParent(@PathVariable Long schoolId, @PathVariable Long parentId) {
        parentService.deactivateParent(schoolId, parentId);
        return ApiResponse.ok(null);
    }

    @GetMapping("/parents/me")
    @PreAuthorize("hasRole('PARENT')")
    public ApiResponse<ParentResponse> getMyProfile() {
        return ApiResponse.ok(parentService.getMyProfile());
    }

    @PatchMapping("/parents/me")
    @PreAuthorize("hasRole('PARENT')")
    public ApiResponse<ParentResponse> completeMyProfile(@Valid @RequestBody CompleteParentProfileRequest request) {
        return ApiResponse.ok(parentService.completeMyProfile(request));
    }
}

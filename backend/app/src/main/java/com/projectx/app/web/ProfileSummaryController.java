package com.projectx.app.web;

import com.projectx.app.dto.ProfileSummaryResponse;
import com.projectx.app.service.ProfileSummaryService;
import com.projectx.common.web.ApiResponse;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

/** Complainant detail card for the grievances table: platform-wide for the platform admin, school-scoped for a school admin. */
@RestController
public class ProfileSummaryController {

    private final ProfileSummaryService profileSummaryService;

    public ProfileSummaryController(ProfileSummaryService profileSummaryService) {
        this.profileSummaryService = profileSummaryService;
    }

    @GetMapping("/platform-admin/users/{userId}/profile-summary")
    @PreAuthorize("hasRole('PLATFORM_ADMIN')")
    public ApiResponse<ProfileSummaryResponse> platformSummary(@PathVariable Long userId) {
        return ApiResponse.ok(profileSummaryService.summary(null, userId));
    }

    @GetMapping("/schools/{schoolId}/users/{userId}/profile-summary")
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN', 'SCHOOL_ADMIN')")
    public ApiResponse<ProfileSummaryResponse> schoolSummary(@PathVariable Long schoolId, @PathVariable Long userId) {
        return ApiResponse.ok(profileSummaryService.summary(schoolId, userId));
    }
}

package com.projectx.attendance.web;

import com.projectx.attendance.dto.StaffDtos.InviteTeacherRequest;
import com.projectx.attendance.dto.StaffDtos.InviteTeacherResponse;
import com.projectx.attendance.dto.StaffDtos.LeadResponse;
import com.projectx.attendance.dto.StaffDtos.TeacherResponse;
import com.projectx.attendance.dto.StaffDtos.UpdateTeacherRequest;
import com.projectx.attendance.service.LeadService;
import com.projectx.attendance.service.TeacherService;
import com.projectx.common.web.ApiResponse;
import com.projectx.common.web.PageResponse;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Teacher management (school admin) and the consent-filtered leads listing (platform admin). */
@RestController
public class StaffController {

    private static final String ADMIN = "hasAnyRole('PLATFORM_ADMIN', 'SCHOOL_ADMIN')";

    private final TeacherService teacherService;
    private final LeadService leadService;

    public StaffController(TeacherService teacherService, LeadService leadService) {
        this.teacherService = teacherService;
        this.leadService = leadService;
    }

    @PostMapping("/schools/{schoolId}/teachers")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize(ADMIN)
    public ApiResponse<InviteTeacherResponse> inviteTeacher(@PathVariable Long schoolId, @Valid @RequestBody InviteTeacherRequest request) {
        return ApiResponse.ok(teacherService.invite(schoolId, request));
    }

    @GetMapping("/schools/{schoolId}/teachers")
    @PreAuthorize(ADMIN)
    public ApiResponse<List<TeacherResponse>> listTeachers(@PathVariable Long schoolId) {
        return ApiResponse.ok(teacherService.list(schoolId));
    }

    @GetMapping("/schools/{schoolId}/teachers/{teacherId}")
    @PreAuthorize(ADMIN)
    public ApiResponse<TeacherResponse> getTeacher(@PathVariable Long schoolId, @PathVariable Long teacherId) {
        return ApiResponse.ok(teacherService.get(schoolId, teacherId));
    }

    @PatchMapping("/schools/{schoolId}/teachers/{teacherId}")
    @PreAuthorize(ADMIN)
    public ApiResponse<TeacherResponse> updateTeacher(@PathVariable Long schoolId, @PathVariable Long teacherId,
                                                      @Valid @RequestBody UpdateTeacherRequest request) {
        return ApiResponse.ok(teacherService.update(schoolId, teacherId, request));
    }

    /** Re-issue the invitation of a teacher who has not accepted yet (INVITED or EXPIRED); the old link stops working. */
    @PostMapping("/schools/{schoolId}/teachers/{teacherId}/resend-invite")
    @PreAuthorize(ADMIN)
    public ApiResponse<InviteTeacherResponse> resendInvite(@PathVariable Long schoolId, @PathVariable Long teacherId) {
        return ApiResponse.ok(teacherService.resendInvite(schoolId, teacherId));
    }

    @GetMapping("/platform-admin/leads")
    @PreAuthorize("hasRole('PLATFORM_ADMIN')")
    public ApiResponse<PageResponse<LeadResponse>> leads(@RequestParam(required = false) Integer page,
                                                          @RequestParam(required = false) Integer size) {
        return ApiResponse.ok(leadService.list(page, size));
    }
}

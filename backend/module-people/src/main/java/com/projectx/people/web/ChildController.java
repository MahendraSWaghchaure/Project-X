package com.projectx.people.web;

import com.projectx.common.web.ApiResponse;
import com.projectx.people.dto.AssignChildRouteStopRequest;
import com.projectx.people.dto.ChildResponse;
import com.projectx.people.dto.CreateChildRequest;
import com.projectx.people.dto.LinkInsurancePolicyRequest;
import com.projectx.people.service.ChildService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ChildController {

    private final ChildService childService;

    public ChildController(ChildService childService) {
        this.childService = childService;
    }

    @PostMapping("/schools/{schoolId}/children")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN', 'SCHOOL_ADMIN')")
    public ApiResponse<ChildResponse> createChildAsSchoolAdmin(@PathVariable Long schoolId,
                                                                 @Valid @RequestBody CreateChildRequest request) {
        return ApiResponse.ok(childService.createChildAsSchoolAdmin(schoolId, request));
    }

    @GetMapping("/schools/{schoolId}/children")
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN', 'SCHOOL_ADMIN')")
    public ApiResponse<List<ChildResponse>> listChildrenForSchool(@PathVariable Long schoolId) {
        return ApiResponse.ok(childService.listChildrenForSchool(schoolId));
    }

    @GetMapping("/schools/{schoolId}/routes/{routeId}/children")
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN', 'SCHOOL_ADMIN', 'DRIVER')")
    public ApiResponse<List<ChildResponse>> listChildrenForRoute(@PathVariable Long schoolId, @PathVariable Long routeId) {
        return ApiResponse.ok(childService.listChildrenForRoute(schoolId, routeId));
    }

    @PostMapping("/parents/me/children")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('PARENT')")
    public ApiResponse<ChildResponse> createChildAsParent(@Valid @RequestBody CreateChildRequest request) {
        return ApiResponse.ok(childService.createChildAsParent(request));
    }

    @GetMapping("/parents/me/children")
    @PreAuthorize("hasRole('PARENT')")
    public ApiResponse<List<ChildResponse>> listMyChildren() {
        return ApiResponse.ok(childService.listMyChildren());
    }

    @PutMapping("/schools/{schoolId}/children/{childId}/route-stop")
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN', 'SCHOOL_ADMIN')")
    public ApiResponse<ChildResponse> assignRouteStop(@PathVariable Long schoolId, @PathVariable Long childId,
                                                        @Valid @RequestBody AssignChildRouteStopRequest request) {
        return ApiResponse.ok(childService.assignRouteStop(schoolId, childId, request));
    }

    @PutMapping("/schools/{schoolId}/children/{childId}/insurance-policy")
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN', 'SCHOOL_ADMIN')")
    public ApiResponse<ChildResponse> linkInsurancePolicy(@PathVariable Long schoolId, @PathVariable Long childId,
                                                            @Valid @RequestBody LinkInsurancePolicyRequest request) {
        return ApiResponse.ok(childService.linkInsurancePolicy(schoolId, childId, request.insurancePolicyId()));
    }
}

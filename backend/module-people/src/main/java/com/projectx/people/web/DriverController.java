package com.projectx.people.web;

import com.projectx.common.web.ApiResponse;
import com.projectx.people.dto.CreateDriverRequest;
import com.projectx.people.dto.DriverResponse;
import com.projectx.people.dto.LinkInsurancePolicyRequest;
import com.projectx.people.dto.UpdateDriverEmploymentStatusRequest;
import com.projectx.people.dto.UpdateDriverRequest;
import com.projectx.people.dto.UpdateDriverVerificationRequest;
import com.projectx.people.service.DriverService;
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
@RequestMapping("/schools/{schoolId}/drivers")
@PreAuthorize("hasAnyRole('PLATFORM_ADMIN', 'SCHOOL_ADMIN')")
public class DriverController {

    private final DriverService driverService;

    public DriverController(DriverService driverService) {
        this.driverService = driverService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<DriverResponse> createDriver(@PathVariable Long schoolId,
                                                      @Valid @RequestBody CreateDriverRequest request) {
        return ApiResponse.ok(driverService.createDriver(schoolId, request));
    }

    @GetMapping
    public ApiResponse<List<DriverResponse>> listDrivers(@PathVariable Long schoolId) {
        return ApiResponse.ok(driverService.listDrivers(schoolId));
    }

    @PatchMapping("/{driverId}/employment-status")
    public ApiResponse<DriverResponse> updateEmploymentStatus(@PathVariable Long schoolId, @PathVariable Long driverId,
                                                                @Valid @RequestBody UpdateDriverEmploymentStatusRequest request) {
        return ApiResponse.ok(driverService.updateEmploymentStatus(schoolId, driverId, request.status()));
    }

    @PatchMapping("/{driverId}/verification")
    public ApiResponse<DriverResponse> updateVerificationStatus(@PathVariable Long schoolId, @PathVariable Long driverId,
                                                                   @Valid @RequestBody UpdateDriverVerificationRequest request) {
        return ApiResponse.ok(driverService.updateVerificationStatus(schoolId, driverId, request.status()));
    }

    @PatchMapping("/{driverId}/insurance-policy")
    public ApiResponse<DriverResponse> linkInsurancePolicy(@PathVariable Long schoolId, @PathVariable Long driverId,
                                                             @Valid @RequestBody LinkInsurancePolicyRequest request) {
        return ApiResponse.ok(driverService.linkInsurancePolicy(schoolId, driverId, request.insurancePolicyId()));
    }

    @PatchMapping("/{driverId}")
    public ApiResponse<DriverResponse> updateDriver(@PathVariable Long schoolId, @PathVariable Long driverId,
                                                      @Valid @RequestBody UpdateDriverRequest request) {
        return ApiResponse.ok(driverService.updateDriver(schoolId, driverId, request));
    }

    @DeleteMapping("/{driverId}")
    public ApiResponse<Void> terminateDriver(@PathVariable Long schoolId, @PathVariable Long driverId) {
        driverService.terminateDriver(schoolId, driverId);
        return ApiResponse.ok(null);
    }

    @PostMapping("/{driverId}/claim")
    public ApiResponse<DriverResponse> claimDriver(@PathVariable Long schoolId, @PathVariable Long driverId) {
        return ApiResponse.ok(driverService.claimDriver(schoolId, driverId));
    }
}

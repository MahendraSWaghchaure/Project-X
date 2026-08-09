package com.projectx.people.web;

import com.projectx.common.web.ApiResponse;
import com.projectx.people.dto.DriverResponse;
import com.projectx.people.dto.DriverSelfRegisterRequest;
import com.projectx.people.service.DriverService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Separate from DriverController because these two endpoints don't fit that class's
 * /schools/{schoolId}/drivers tenant scoping - registration is public (no school, no auth yet),
 * and the independent-driver pool is cross-tenant by definition (see SecurityConfig for the
 * public permit on /drivers/register).
 */
@RestController
public class DriverRegistrationController {

    private final DriverService driverService;

    public DriverRegistrationController(DriverService driverService) {
        this.driverService = driverService;
    }

    @PostMapping("/drivers/register")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<DriverResponse> selfRegister(@Valid @RequestBody DriverSelfRegisterRequest request) {
        return ApiResponse.ok(driverService.selfRegister(request));
    }

    @GetMapping("/drivers/independent")
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN', 'SCHOOL_ADMIN')")
    public ApiResponse<List<DriverResponse>> listIndependentDrivers() {
        return ApiResponse.ok(driverService.listIndependentDrivers());
    }
}

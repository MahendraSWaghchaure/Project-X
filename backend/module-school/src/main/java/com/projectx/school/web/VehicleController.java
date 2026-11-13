package com.projectx.school.web;

import com.projectx.common.web.ApiResponse;
import com.projectx.school.dto.AssignVehicleDriverRequest;
import com.projectx.school.dto.CreateVehicleRequest;
import com.projectx.school.dto.LinkInsurancePolicyRequest;
import com.projectx.school.dto.UpdateVehicleRequest;
import com.projectx.school.dto.UpdateVehicleStatusRequest;
import com.projectx.school.dto.VehicleResponse;
import com.projectx.school.service.VehicleService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/schools/{schoolId}/vehicles")
@PreAuthorize("hasAnyRole('PLATFORM_ADMIN', 'SCHOOL_ADMIN')")
public class VehicleController {

    private final VehicleService vehicleService;

    public VehicleController(VehicleService vehicleService) {
        this.vehicleService = vehicleService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<VehicleResponse> createVehicle(@PathVariable Long schoolId,
                                                        @Valid @RequestBody CreateVehicleRequest request) {
        return ApiResponse.ok(vehicleService.createVehicle(schoolId, request));
    }

    @GetMapping
    public ApiResponse<List<VehicleResponse>> listVehicles(@PathVariable Long schoolId) {
        return ApiResponse.ok(vehicleService.listVehicles(schoolId));
    }

    @GetMapping("/{vehicleId}")
    public ApiResponse<VehicleResponse> getVehicle(@PathVariable Long schoolId, @PathVariable Long vehicleId) {
        return ApiResponse.ok(vehicleService.getVehicle(schoolId, vehicleId));
    }

    /** Allocate (or, with a null driverId, release) the vehicle's driver. */
    @PutMapping("/{vehicleId}/driver")
    public ApiResponse<VehicleResponse> assignDriver(@PathVariable Long schoolId, @PathVariable Long vehicleId,
                                                      @RequestBody AssignVehicleDriverRequest request) {
        return ApiResponse.ok(vehicleService.assignDriver(schoolId, vehicleId, request.driverId()));
    }

    @PatchMapping("/{vehicleId}/status")
    public ApiResponse<VehicleResponse> updateStatus(@PathVariable Long schoolId, @PathVariable Long vehicleId,
                                                       @Valid @RequestBody UpdateVehicleStatusRequest request) {
        return ApiResponse.ok(vehicleService.updateStatus(schoolId, vehicleId, request.status()));
    }

    @PatchMapping("/{vehicleId}/insurance-policy")
    public ApiResponse<VehicleResponse> linkInsurancePolicy(@PathVariable Long schoolId, @PathVariable Long vehicleId,
                                                              @Valid @RequestBody LinkInsurancePolicyRequest request) {
        return ApiResponse.ok(vehicleService.linkInsurancePolicy(schoolId, vehicleId, request.insurancePolicyId()));
    }

    @PatchMapping("/{vehicleId}")
    public ApiResponse<VehicleResponse> updateVehicle(@PathVariable Long schoolId, @PathVariable Long vehicleId,
                                                        @Valid @RequestBody UpdateVehicleRequest request) {
        return ApiResponse.ok(vehicleService.updateVehicle(schoolId, vehicleId, request));
    }

    @DeleteMapping("/{vehicleId}")
    public ApiResponse<Void> retireVehicle(@PathVariable Long schoolId, @PathVariable Long vehicleId) {
        vehicleService.retireVehicle(schoolId, vehicleId);
        return ApiResponse.ok(null);
    }
}

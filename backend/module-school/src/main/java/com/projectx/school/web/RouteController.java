package com.projectx.school.web;

import com.projectx.common.web.ApiResponse;
import com.projectx.school.dto.AssignRouteRequest;
import com.projectx.school.dto.CreateRouteRequest;
import com.projectx.school.dto.RouteResponse;
import com.projectx.school.dto.UpdateRouteZoneRequest;
import com.projectx.school.service.RouteService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/schools/{schoolId}/routes")
@PreAuthorize("hasAnyRole('PLATFORM_ADMIN', 'SCHOOL_ADMIN')")
public class RouteController {

    private final RouteService routeService;

    public RouteController(RouteService routeService) {
        this.routeService = routeService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<RouteResponse> createRoute(@PathVariable Long schoolId,
                                                   @Valid @RequestBody CreateRouteRequest request) {
        return ApiResponse.ok(routeService.createRoute(schoolId, request));
    }

    @GetMapping
    public ApiResponse<List<RouteResponse>> listRoutes(@PathVariable Long schoolId) {
        return ApiResponse.ok(routeService.listRoutes(schoolId));
    }

    @GetMapping("/{routeId}")
    public ApiResponse<RouteResponse> getRoute(@PathVariable Long schoolId, @PathVariable Long routeId) {
        return ApiResponse.ok(routeService.getRoute(schoolId, routeId));
    }

    @PutMapping("/{routeId}/assignment")
    public ApiResponse<RouteResponse> assignRoute(@PathVariable Long schoolId, @PathVariable Long routeId,
                                                    @Valid @RequestBody AssignRouteRequest request) {
        return ApiResponse.ok(routeService.assignRoute(schoolId, routeId, request));
    }

    @PutMapping("/{routeId}/zone")
    public ApiResponse<RouteResponse> updateZone(@PathVariable Long schoolId, @PathVariable Long routeId,
                                                   @Valid @RequestBody UpdateRouteZoneRequest request) {
        return ApiResponse.ok(routeService.updateZone(schoolId, routeId, request));
    }
}

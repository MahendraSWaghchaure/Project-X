package com.projectx.tracking.web;

import com.projectx.common.web.ApiResponse;
import com.projectx.tracking.domain.TripDirection;
import com.projectx.tracking.dto.CreateTripRequest;
import com.projectx.tracking.dto.TripResponse;
import com.projectx.tracking.service.TripService;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TripController {

    private final TripService tripService;

    public TripController(TripService tripService) {
        this.tripService = tripService;
    }

    @PostMapping("/schools/{schoolId}/trips")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN', 'SCHOOL_ADMIN')")
    public ApiResponse<TripResponse> scheduleTrip(@PathVariable Long schoolId, @Valid @RequestBody CreateTripRequest request) {
        return ApiResponse.ok(tripService.scheduleTrip(schoolId, request));
    }

    @GetMapping("/schools/{schoolId}/trips")
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN', 'SCHOOL_ADMIN')")
    public ApiResponse<List<TripResponse>> listTripsForSchool(@PathVariable Long schoolId,
                                                                @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ApiResponse.ok(tripService.listTripsForSchool(schoolId, date));
    }

    @GetMapping("/drivers/me/trips")
    @PreAuthorize("hasRole('DRIVER')")
    public ApiResponse<List<TripResponse>> listMyTripsAsDriver(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ApiResponse.ok(tripService.listMyTripsAsDriver(date));
    }

    @GetMapping("/trips/{tripId}")
    public ApiResponse<TripResponse> getTrip(@PathVariable Long tripId) {
        return ApiResponse.ok(tripService.getTrip(tripId));
    }

    @PostMapping("/trips/{tripId}/start")
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN', 'SCHOOL_ADMIN', 'DRIVER')")
    public ApiResponse<TripResponse> startTrip(@PathVariable Long tripId) {
        return ApiResponse.ok(tripService.startTrip(tripId));
    }

    @PostMapping("/trips/{tripId}/complete")
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN', 'SCHOOL_ADMIN', 'DRIVER')")
    public ApiResponse<TripResponse> completeTrip(@PathVariable Long tripId) {
        return ApiResponse.ok(tripService.completeTrip(tripId));
    }

    @PostMapping("/trips/{tripId}/cancel")
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN', 'SCHOOL_ADMIN', 'DRIVER')")
    public ApiResponse<TripResponse> cancelTrip(@PathVariable Long tripId) {
        return ApiResponse.ok(tripService.cancelTrip(tripId));
    }

    @GetMapping("/parents/me/children/{childId}/trips/today")
    @PreAuthorize("hasRole('PARENT')")
    public ApiResponse<TripResponse> getTodaysTripForMyChild(@PathVariable Long childId,
                                                               @RequestParam TripDirection direction) {
        return ApiResponse.ok(tripService.getTodaysTripForMyChild(childId, direction));
    }
}

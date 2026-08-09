package com.projectx.tracking.web;

import com.projectx.common.web.ApiResponse;
import com.projectx.tracking.dto.ChildTripEventResponse;
import com.projectx.tracking.dto.RecordChildTripEventRequest;
import com.projectx.tracking.service.ChildTripEventService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/trips/{tripId}")
public class ChildTripEventController {

    private final ChildTripEventService childTripEventService;

    public ChildTripEventController(ChildTripEventService childTripEventService) {
        this.childTripEventService = childTripEventService;
    }

    @PostMapping("/child-events")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN', 'SCHOOL_ADMIN', 'DRIVER')")
    public ApiResponse<ChildTripEventResponse> recordDriverMarkedEvent(@PathVariable Long tripId,
                                                                        @Valid @RequestBody RecordChildTripEventRequest request) {
        return ApiResponse.ok(childTripEventService.recordDriverMarkedEvent(tripId, request.childId(), request.eventType()));
    }

    @PostMapping("/children/{childId}/home-arrival")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('PARENT')")
    public ApiResponse<ChildTripEventResponse> recordParentHomeArrival(@PathVariable Long tripId, @PathVariable Long childId) {
        return ApiResponse.ok(childTripEventService.recordParentHomeArrival(tripId, childId));
    }

    @GetMapping("/child-events")
    public ApiResponse<List<ChildTripEventResponse>> listForTrip(@PathVariable Long tripId) {
        return ApiResponse.ok(childTripEventService.listForTrip(tripId));
    }
}

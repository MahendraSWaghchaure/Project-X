package com.projectx.tracking.web;

import com.projectx.common.web.ApiResponse;
import com.projectx.tracking.dto.ChildTripEventResponse;
import com.projectx.tracking.service.ChildTripEventService;
import java.time.LocalDate;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AttendanceController {

    private final ChildTripEventService childTripEventService;

    public AttendanceController(ChildTripEventService childTripEventService) {
        this.childTripEventService = childTripEventService;
    }

    @GetMapping("/parents/me/children/{childId}/attendance")
    @PreAuthorize("hasRole('PARENT')")
    public ApiResponse<List<ChildTripEventResponse>> getAttendanceHistory(
            @PathVariable Long childId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ApiResponse.ok(childTripEventService.getAttendanceHistory(childId, from, to));
    }
}

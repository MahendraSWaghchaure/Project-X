package com.projectx.tracking.web;

import com.projectx.common.web.ApiResponse;
import com.projectx.tracking.dto.SetChildAbsenceRequest;
import com.projectx.tracking.service.ChildAbsenceService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
public class ChildAbsenceController {

    private final ChildAbsenceService childAbsenceService;

    public ChildAbsenceController(ChildAbsenceService childAbsenceService) {
        this.childAbsenceService = childAbsenceService;
    }

    @PutMapping("/parents/me/children/{childId}/absence")
    @PreAuthorize("hasRole('PARENT')")
    public ApiResponse<Void> markAbsent(@PathVariable Long childId, @Valid @RequestBody SetChildAbsenceRequest request) {
        childAbsenceService.markAbsent(childId, request.date());
        return ApiResponse.ok(null);
    }

    @DeleteMapping("/parents/me/children/{childId}/absence")
    @PreAuthorize("hasRole('PARENT')")
    public ApiResponse<Void> clearAbsence(@PathVariable Long childId,
                                            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        childAbsenceService.clearAbsence(childId, date);
        return ApiResponse.ok(null);
    }
}

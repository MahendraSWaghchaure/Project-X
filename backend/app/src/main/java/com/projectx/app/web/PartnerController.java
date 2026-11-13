package com.projectx.app.web;

import com.projectx.app.domain.PartnerType;
import com.projectx.app.dto.PartnerDtos.PartnerRequest;
import com.projectx.app.dto.PartnerDtos.PartnerResponse;
import com.projectx.app.service.PartnerService;
import com.projectx.common.web.ApiResponse;
import com.projectx.common.web.PageResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** F1: platform-admin-only CRUD/list for future-partner leads (insurance company / legal advisor / RTO agent). */
@RestController
@RequestMapping("/platform-admin/partners")
@PreAuthorize("hasRole('PLATFORM_ADMIN')")
public class PartnerController {

    private final PartnerService partnerService;

    public PartnerController(PartnerService partnerService) {
        this.partnerService = partnerService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<PartnerResponse> create(@Valid @RequestBody PartnerRequest request) {
        return ApiResponse.ok(partnerService.create(request));
    }

    @GetMapping
    public ApiResponse<PageResponse<PartnerResponse>> list(@RequestParam(required = false) PartnerType type,
                                                            @RequestParam(required = false) Integer page,
                                                            @RequestParam(required = false) Integer size) {
        return ApiResponse.ok(partnerService.list(type, page, size));
    }

    @GetMapping("/{id}")
    public ApiResponse<PartnerResponse> get(@PathVariable Long id) {
        return ApiResponse.ok(partnerService.get(id));
    }

    @PatchMapping("/{id}")
    public ApiResponse<PartnerResponse> update(@PathVariable Long id, @Valid @RequestBody PartnerRequest request) {
        return ApiResponse.ok(partnerService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        partnerService.delete(id);
        return ApiResponse.ok(null);
    }
}

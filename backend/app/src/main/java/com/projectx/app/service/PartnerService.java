package com.projectx.app.service;

import com.projectx.app.domain.Partner;
import com.projectx.app.domain.PartnerType;
import com.projectx.app.dto.PartnerDtos.PartnerRequest;
import com.projectx.app.dto.PartnerDtos.PartnerResponse;
import com.projectx.app.repo.PartnerRepository;
import com.projectx.common.events.AuditEvent;
import com.projectx.common.exception.ResourceNotFoundException;
import com.projectx.common.web.PageResponse;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * F1: platform-admin-only lead-capture for future partners (insurance company / legal advisor /
 * RTO agent). No onboarding, policy issuance, payment or driver-facing flow - see Partner's javadoc.
 */
@Service
@Transactional
public class PartnerService {

    private static final int MAX_PAGE_SIZE = 100;
    private static final int DEFAULT_PAGE_SIZE = 20;

    private final PartnerRepository partnerRepository;
    private final ApplicationEventPublisher eventPublisher;

    public PartnerService(PartnerRepository partnerRepository, ApplicationEventPublisher eventPublisher) {
        this.partnerRepository = partnerRepository;
        this.eventPublisher = eventPublisher;
    }

    public PartnerResponse create(PartnerRequest request) {
        Partner partner = partnerRepository.save(new Partner(request.type(), request.name().trim(),
                blankToNull(request.contactEmail()), blankToNull(request.contactPhone()), blankToNull(request.notes())));
        eventPublisher.publishEvent(AuditEvent.of("PARTNER_CREATED", "PARTNER", partner.getId(), null,
                "type=" + request.type() + " name=" + partner.getName()));
        return PartnerResponse.from(partner);
    }

    @Transactional(readOnly = true)
    public PageResponse<PartnerResponse> list(PartnerType type, Integer page, Integer size) {
        var pageable = PageRequest.of(Math.max(page == null ? 0 : page, 0), clampSize(size), Sort.by(Sort.Order.desc("createdAt")));
        var result = type != null ? partnerRepository.findByType(type, pageable) : partnerRepository.findAll(pageable);
        return PageResponse.from(result.map(PartnerResponse::from));
    }

    @Transactional(readOnly = true)
    public PartnerResponse get(Long id) {
        return PartnerResponse.from(require(id));
    }

    public PartnerResponse update(Long id, PartnerRequest request) {
        Partner partner = require(id);
        partner.updateDetails(request.name().trim(), blankToNull(request.contactEmail()), blankToNull(request.contactPhone()),
                blankToNull(request.notes()));
        eventPublisher.publishEvent(AuditEvent.of("PARTNER_UPDATED", "PARTNER", partner.getId(), null, "name=" + partner.getName()));
        return PartnerResponse.from(partner);
    }

    public void delete(Long id) {
        Partner partner = require(id);
        partnerRepository.delete(partner);
        eventPublisher.publishEvent(AuditEvent.of("PARTNER_DELETED", "PARTNER", id, null, "type=" + partner.getType()));
    }

    private Partner require(Long id) {
        return partnerRepository.findById(id).orElseThrow(() -> ResourceNotFoundException.of("Partner", id));
    }

    private static int clampSize(Integer size) {
        return Math.min(Math.max(size == null ? DEFAULT_PAGE_SIZE : size, 1), MAX_PAGE_SIZE);
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }
}

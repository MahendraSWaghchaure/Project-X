package com.projectx.payment.service;

import com.projectx.common.exception.ConflictException;
import com.projectx.common.exception.ForbiddenOperationException;
import com.projectx.common.exception.ResourceNotFoundException;
import com.projectx.common.security.CurrentUser;
import com.projectx.common.security.CurrentUserContext;
import com.projectx.people.domain.ParentProfile;
import com.projectx.people.repo.ParentProfileRepository;
import com.projectx.payment.domain.FeeInvoice;
import com.projectx.payment.domain.LateFeeWaiverRequest;
import com.projectx.payment.domain.WaiverStatus;
import com.projectx.payment.dto.CreateWaiverRequest;
import com.projectx.payment.dto.ReviewWaiverRequest;
import com.projectx.payment.dto.WaiverResponse;
import com.projectx.payment.repo.FeeInvoiceRepository;
import com.projectx.payment.repo.LateFeeWaiverRequestRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Late-fee waivers are reviewed by the platform admin (a deliberate policy choice - see the product spec), not the school. */
@Service
@Transactional
public class LateFeeWaiverService {

    private final LateFeeWaiverRequestRepository waiverRepository;
    private final FeeInvoiceRepository feeInvoiceRepository;
    private final ParentProfileRepository parentProfileRepository;

    public LateFeeWaiverService(LateFeeWaiverRequestRepository waiverRepository, FeeInvoiceRepository feeInvoiceRepository,
                                 ParentProfileRepository parentProfileRepository) {
        this.waiverRepository = waiverRepository;
        this.feeInvoiceRepository = feeInvoiceRepository;
        this.parentProfileRepository = parentProfileRepository;
    }

    public WaiverResponse create(CreateWaiverRequest request) {
        ParentProfile parent = currentParent();
        FeeInvoice invoice = feeInvoiceRepository.findById(request.feeInvoiceId())
                .orElseThrow(() -> ResourceNotFoundException.of("Invoice", request.feeInvoiceId()));
        if (!invoice.getParentId().equals(parent.getId())) {
            throw new ForbiddenOperationException("Not your invoice");
        }
        if (invoice.getLateFeeAmount().signum() <= 0) {
            throw new ConflictException("This invoice has no late fee to waive");
        }
        waiverRepository.findByFeeInvoiceIdAndStatus(invoice.getId(), WaiverStatus.PENDING)
                .ifPresent(existing -> {
                    throw new ConflictException("A waiver request for this invoice is already pending review");
                });

        LateFeeWaiverRequest waiver = new LateFeeWaiverRequest(invoice.getId(), parent.getId(), invoice.getSchoolId(), request.reason());
        return WaiverResponse.from(waiverRepository.save(waiver));
    }

    @Transactional(readOnly = true)
    public List<WaiverResponse> listMine() {
        ParentProfile parent = currentParent();
        return waiverRepository.findByParentIdOrderByCreatedAtDesc(parent.getId()).stream().map(WaiverResponse::from).toList();
    }

    /** Platform admin only - see class javadoc for why waivers aren't a per-school decision. */
    @Transactional(readOnly = true)
    public List<WaiverResponse> listAll() {
        return waiverRepository.findAllByOrderByCreatedAtDesc().stream().map(WaiverResponse::from).toList();
    }

    public WaiverResponse review(Long waiverId, ReviewWaiverRequest request) {
        if (request.status() == WaiverStatus.PENDING) {
            throw new IllegalArgumentException("status must be APPROVED or REJECTED");
        }
        CurrentUser reviewer = CurrentUserContext.get();
        LateFeeWaiverRequest waiver = waiverRepository.findById(waiverId)
                .orElseThrow(() -> ResourceNotFoundException.of("Waiver request", waiverId));
        if (waiver.getStatus() != WaiverStatus.PENDING) {
            throw new ConflictException("This waiver request was already reviewed");
        }

        if (request.status() == WaiverStatus.APPROVED) {
            FeeInvoice invoice = feeInvoiceRepository.findById(waiver.getFeeInvoiceId())
                    .orElseThrow(() -> ResourceNotFoundException.of("Invoice", waiver.getFeeInvoiceId()));
            invoice.waiveLateFee();
            waiver.approve(reviewer.userId(), request.reviewNote());
        } else {
            waiver.reject(reviewer.userId(), request.reviewNote());
        }
        return WaiverResponse.from(waiver);
    }

    private ParentProfile currentParent() {
        CurrentUser user = CurrentUserContext.get();
        return parentProfileRepository.findByUserId(user.userId())
                .orElseThrow(() -> new ForbiddenOperationException("No parent profile for the current user"));
    }
}

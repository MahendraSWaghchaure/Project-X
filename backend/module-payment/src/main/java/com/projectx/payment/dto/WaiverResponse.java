package com.projectx.payment.dto;

import com.projectx.payment.domain.LateFeeWaiverRequest;
import com.projectx.payment.domain.WaiverStatus;
import java.time.Instant;

public record WaiverResponse(
        Long id, Long feeInvoiceId, Long parentId, Long schoolId, String reason, WaiverStatus status,
        Long reviewedByUserId, Instant reviewedAt, String reviewNote, Instant createdAt) {

    public static WaiverResponse from(LateFeeWaiverRequest request) {
        return new WaiverResponse(request.getId(), request.getFeeInvoiceId(), request.getParentId(), request.getSchoolId(),
                request.getReason(), request.getStatus(), request.getReviewedByUserId(), request.getReviewedAt(),
                request.getReviewNote(), request.getCreatedAt());
    }
}

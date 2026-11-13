package com.projectx.notification.service;

/** Module-internal: published inside the transaction that stored the inquiry, handled AFTER_COMMIT (so a rolled-back insert notifies nobody). */
public record InquiryReceivedEvent(Long inquiryId, String name, String contact, String message) {
}

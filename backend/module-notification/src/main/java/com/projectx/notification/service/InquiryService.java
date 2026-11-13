package com.projectx.notification.service;

import com.projectx.common.events.AuditEvent;
import com.projectx.common.exception.ResourceNotFoundException;
import com.projectx.common.security.CurrentUserContext;
import com.projectx.common.security.Role;
import com.projectx.common.web.PageResponse;
import com.projectx.identity.domain.UserAccount;
import com.projectx.identity.repo.UserAccountRepository;
import com.projectx.notification.domain.Inquiry;
import com.projectx.notification.domain.InquiryStatus;
import com.projectx.notification.dto.InquiryDtos.CreateInquiryRequest;
import com.projectx.notification.dto.InquiryDtos.InquiryReceipt;
import com.projectx.notification.dto.InquiryDtos.InquiryResponse;
import com.projectx.notification.repo.InquiryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Public inquiries: stored, then the platform admins are told through the existing dispatcher (async, AFTER_COMMIT, on
 * the notificationExecutor whose MdcTaskDecorator carries the request's X-Request-Id).
 */
@Service
public class InquiryService {

    private static final Logger log = LoggerFactory.getLogger(InquiryService.class);
    private static final int MAX_PAGE = 100;
    private static final int MAX_ADMINS_NOTIFIED = 20;

    private final InquiryRepository inquiryRepository;
    private final UserAccountRepository userAccountRepository;
    private final NotificationDispatchService dispatchService;
    private final ApplicationEventPublisher eventPublisher;

    public InquiryService(InquiryRepository inquiryRepository, UserAccountRepository userAccountRepository,
                          NotificationDispatchService dispatchService, ApplicationEventPublisher eventPublisher) {
        this.inquiryRepository = inquiryRepository;
        this.userAccountRepository = userAccountRepository;
        this.dispatchService = dispatchService;
        this.eventPublisher = eventPublisher;
    }

    /** Honeypot hits are acknowledged but never stored. Control characters are stripped so a message cannot forge log/SMS lines. */
    @Transactional
    public InquiryReceipt submit(CreateInquiryRequest request) {
        if (request.website() != null && !request.website().isBlank()) {
            return new InquiryReceipt(true);
        }
        Inquiry saved = inquiryRepository.save(new Inquiry(clean(request.name()), clean(request.contact()), clean(request.message())));
        eventPublisher.publishEvent(new InquiryReceivedEvent(saved.getId(), saved.getName(), saved.getContact(), saved.getMessage()));
        return new InquiryReceipt(true);
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Async("notificationExecutor")
    public void onInquiryReceived(InquiryReceivedEvent event) {
        try {
            String subject = "New inquiry from " + event.name();
            String body = "Inquiry #" + event.inquiryId() + " from " + event.name() + " (" + event.contact() + "): "
                    + truncate(event.message(), 300);
            for (UserAccount admin : userAccountRepository
                    .findByRole(Role.PLATFORM_ADMIN, PageRequest.of(0, MAX_ADMINS_NOTIFIED)).getContent()) {
                if (admin.isActive()) {
                    dispatchService.dispatch(admin.getId(), "inquiry.NEW", subject, body);
                }
            }
        } catch (Exception e) {
            log.error("Could not notify platform admins of inquiry {}", event.inquiryId(), e);
        }
    }

    @Transactional(readOnly = true)
    public PageResponse<InquiryResponse> list(InquiryStatus status, Integer page, Integer size) {
        var pageable = PageRequest.of(Math.max(page == null ? 0 : page, 0), Math.min(Math.max(size == null ? 30 : size, 1), MAX_PAGE),
                Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id")));
        var result = status == null ? inquiryRepository.findAll(pageable) : inquiryRepository.findByStatus(status, pageable);
        return PageResponse.from(result.map(InquiryResponse::from));
    }

    @Transactional
    public InquiryResponse updateStatus(Long id, InquiryStatus status) {
        Inquiry inquiry = inquiryRepository.findById(id).orElseThrow(() -> ResourceNotFoundException.of("Inquiry", id));
        inquiry.setStatus(status, CurrentUserContext.get().userId());
        eventPublisher.publishEvent(AuditEvent.of("INQUIRY_STATUS_CHANGED", "INQUIRY", id, null, "status=" + status));
        return InquiryResponse.from(inquiry);
    }

    @Transactional(readOnly = true)
    public long countNew() {
        return inquiryRepository.countByStatus(InquiryStatus.NEW);
    }

    private static String clean(String s) {
        return s.replaceAll("[\\p{Cntrl}&&[^\\n]]", " ").trim();
    }

    private static String truncate(String s, int max) {
        return s.length() <= max ? s : s.substring(0, max) + "...";
    }
}

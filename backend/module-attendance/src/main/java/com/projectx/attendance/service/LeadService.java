package com.projectx.attendance.service;

import com.projectx.attendance.domain.Student;
import com.projectx.attendance.dto.StaffDtos.LeadResponse;
import com.projectx.attendance.repo.StudentRepository;
import com.projectx.common.events.AuditEvent;
import com.projectx.common.web.PageResponse;
import com.projectx.school.domain.School;
import com.projectx.school.repo.SchoolRepository;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Platform-admin only (enforced at the controller). DPDP: students are minors, so a guardian
 * contact is a lead ONLY if marketing_consent = true AND the student is not already a Sunju
 * customer (child_id IS NULL) - the filter is the repository method itself, not a post-filter, so
 * no code path can fetch unconsented rows. Each listing is audited (who looked at guardian PII, when).
 */
@Service
@Transactional(readOnly = true)
public class LeadService {

    private final StudentRepository studentRepository;
    private final SchoolRepository schoolRepository;
    private final ApplicationEventPublisher eventPublisher;

    public LeadService(StudentRepository studentRepository, SchoolRepository schoolRepository,
                       ApplicationEventPublisher eventPublisher) {
        this.studentRepository = studentRepository;
        this.schoolRepository = schoolRepository;
        this.eventPublisher = eventPublisher;
    }

    public PageResponse<LeadResponse> list(Integer page, Integer size) {
        var pageable = PageRequest.of(Math.max(page == null ? 0 : page, 0), StudentService.clampSize(size), Sort.by("id"));
        var students = studentRepository.findByMarketingConsentTrueAndChildIdIsNull(pageable);

        Set<Long> schoolIds = students.getContent().stream().map(Student::getSchoolId).collect(Collectors.toSet());
        Map<Long, String> schoolNames = new HashMap<>();
        for (School school : schoolRepository.findAllById(schoolIds)) {
            schoolNames.put(school.getId(), school.getName());
        }
        eventPublisher.publishEvent(AuditEvent.of("LEADS_VIEWED", "LEAD", null, null,
                "page=" + students.getNumber() + " size=" + students.getNumberOfElements()));
        return PageResponse.from(students.map(s -> new LeadResponse(s.getId(), s.getGuardianName(), s.getGuardianPhone(),
                s.getGuardianEmail(), schoolNames.get(s.getSchoolId()), s.getSchoolId(), s.getLeadSource())));
    }
}

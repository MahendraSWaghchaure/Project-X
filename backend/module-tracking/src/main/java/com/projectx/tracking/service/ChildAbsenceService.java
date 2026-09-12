package com.projectx.tracking.service;

import com.projectx.common.exception.ForbiddenOperationException;
import com.projectx.common.exception.ResourceNotFoundException;
import com.projectx.common.security.CurrentUserContext;
import com.projectx.people.domain.Child;
import com.projectx.people.domain.ParentProfile;
import com.projectx.people.repo.ChildRepository;
import com.projectx.people.repo.ParentProfileRepository;
import com.projectx.people.service.GuardianAccessService;
import com.projectx.tracking.domain.ChildAbsence;
import com.projectx.tracking.repo.ChildAbsenceRepository;
import java.time.LocalDate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** A parent's advance "not riding today/tomorrow" toggle, so the driver's manifest doesn't show a phantom pickup. */
@Service
@Transactional
public class ChildAbsenceService {

    private final ChildAbsenceRepository childAbsenceRepository;
    private final ChildRepository childRepository;
    private final ParentProfileRepository parentProfileRepository;
    private final GuardianAccessService guardianAccessService;

    public ChildAbsenceService(ChildAbsenceRepository childAbsenceRepository, ChildRepository childRepository,
                                ParentProfileRepository parentProfileRepository, GuardianAccessService guardianAccessService) {
        this.childAbsenceRepository = childAbsenceRepository;
        this.childRepository = childRepository;
        this.parentProfileRepository = parentProfileRepository;
        this.guardianAccessService = guardianAccessService;
    }

    public void markAbsent(Long childId, LocalDate date) {
        Child child = authorizeGuardian(childId);
        if (!childAbsenceRepository.existsByChildIdAndAbsenceDate(childId, date)) {
            childAbsenceRepository.save(new ChildAbsence(child.getId(), date, CurrentUserContext.get().userId()));
        }
    }

    public void clearAbsence(Long childId, LocalDate date) {
        authorizeGuardian(childId);
        childAbsenceRepository.findByChildIdAndAbsenceDate(childId, date).ifPresent(childAbsenceRepository::delete);
    }

    private Child authorizeGuardian(Long childId) {
        ParentProfile parent = parentProfileRepository.findByUserId(CurrentUserContext.get().userId())
                .orElseThrow(() -> new ForbiddenOperationException("No parent profile for current user"));
        Child child = childRepository.findById(childId).orElseThrow(() -> ResourceNotFoundException.of("Child", childId));
        if (!guardianAccessService.isGuardian(child, parent.getId())) {
            throw new ForbiddenOperationException("Not a guardian of this child");
        }
        return child;
    }
}

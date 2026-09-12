package com.projectx.people.service;

import com.projectx.people.domain.Child;
import com.projectx.people.domain.ChildGuardian;
import com.projectx.people.repo.ChildGuardianRepository;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Single source of truth for "is this parent allowed to act as a guardian of this child," now that
 * a child can have any number of guardians beyond its primary one (see child_guardians /
 * ChildGuardian). Used across module-people, module-tracking, and module-payment - centralizing it
 * here means the primary-plus-additional-guardians check only needs to be right in one place.
 */
@Service
@Transactional(readOnly = true)
public class GuardianAccessService {

    private final ChildGuardianRepository childGuardianRepository;

    public GuardianAccessService(ChildGuardianRepository childGuardianRepository) {
        this.childGuardianRepository = childGuardianRepository;
    }

    public boolean isGuardian(Child child, Long parentId) {
        if (parentId == null) {
            return false;
        }
        return child.getPrimaryParentId().equals(parentId)
                || childGuardianRepository.existsByChildIdAndParentId(child.getId(), parentId);
    }

    /** Primary first, then every additional guardian - no particular order beyond that among the additional ones. */
    public List<Long> allGuardianIds(Child child) {
        List<Long> ids = new ArrayList<>();
        ids.add(child.getPrimaryParentId());
        for (ChildGuardian guardian : childGuardianRepository.findByChildId(child.getId())) {
            ids.add(guardian.getParentId());
        }
        return ids;
    }

    @Transactional
    public void addGuardian(Long childId, Long parentId) {
        childGuardianRepository.save(new ChildGuardian(childId, parentId));
    }
}

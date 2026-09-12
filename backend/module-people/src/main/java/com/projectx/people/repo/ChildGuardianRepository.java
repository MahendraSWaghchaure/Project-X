package com.projectx.people.repo;

import com.projectx.people.domain.ChildGuardian;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChildGuardianRepository extends JpaRepository<ChildGuardian, Long> {

    List<ChildGuardian> findByChildId(Long childId);

    boolean existsByChildIdAndParentId(Long childId, Long parentId);
}

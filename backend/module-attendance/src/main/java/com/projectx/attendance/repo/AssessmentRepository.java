package com.projectx.attendance.repo;

import com.projectx.attendance.domain.Assessment;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AssessmentRepository extends JpaRepository<Assessment, Long> {

    /** Newest first via the Pageable's sort; always called with a capped page. */
    List<Assessment> findByStudentId(Long studentId, Pageable pageable);
}

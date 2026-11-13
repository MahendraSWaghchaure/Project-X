package com.projectx.attendance.repo;

import com.projectx.attendance.domain.StudentComment;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StudentCommentRepository extends JpaRepository<StudentComment, Long> {

    List<StudentComment> findByStudentId(Long studentId, Pageable pageable);
}

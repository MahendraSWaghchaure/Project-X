package com.projectx.attendance.repo;

import com.projectx.attendance.domain.AttendanceCorrectionRequest;
import com.projectx.attendance.domain.CorrectionStatus;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AttendanceCorrectionRequestRepository extends JpaRepository<AttendanceCorrectionRequest, Long> {

    Optional<AttendanceCorrectionRequest> findByIdAndSchoolId(Long id, Long schoolId);

    Page<AttendanceCorrectionRequest> findBySchoolId(Long schoolId, Pageable pageable);

    Page<AttendanceCorrectionRequest> findBySchoolIdAndStatus(Long schoolId, CorrectionStatus status, Pageable pageable);

    Page<AttendanceCorrectionRequest> findBySchoolIdAndRequestedByUserId(Long schoolId, Long requestedByUserId, Pageable pageable);

    Page<AttendanceCorrectionRequest> findBySchoolIdAndRequestedByUserIdAndStatus(
            Long schoolId, Long requestedByUserId, CorrectionStatus status, Pageable pageable);

    long countBySchoolIdAndStatus(Long schoolId, CorrectionStatus status);

    /** Does this student already have a request pending for this day? (one outstanding correction at a time, per row.) */
    boolean existsByClassroomIdAndStudentIdAndDateAndStatus(Long classroomId, Long studentId, java.time.LocalDate date, CorrectionStatus status);
}

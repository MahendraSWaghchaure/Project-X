package com.projectx.attendance.repo;

import com.projectx.attendance.domain.Classroom;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ClassroomRepository extends JpaRepository<Classroom, Long> {

    /** Always called with a capped Pageable (a school has tens of classrooms; the cap is abuse protection). */
    List<Classroom> findBySchoolId(Long schoolId, Pageable pageable);

    /** Restricted to a set of ids (a teacher's own classes), capped by the Pageable. */
    List<Classroom> findBySchoolIdAndIdIn(Long schoolId, Collection<Long> ids, Pageable pageable);

    Optional<Classroom> findByIdAndSchoolId(Long id, Long schoolId);

    /** At most one (DB-unique partial index). */
    Optional<Classroom> findByClassTeacherId(Long classTeacherId);

    /** Class-teacher assignments of a page of teachers - one query. */
    List<Classroom> findByClassTeacherIdIn(Collection<Long> classTeacherIds);
}

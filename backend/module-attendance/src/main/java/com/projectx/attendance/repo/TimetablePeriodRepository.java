package com.projectx.attendance.repo;

import com.projectx.attendance.domain.TimetablePeriod;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TimetablePeriodRepository extends JpaRepository<TimetablePeriod, Long> {

    /** Callers pass a capped Pageable: a classroom / teacher has at most a few dozen weekly periods. */
    List<TimetablePeriod> findByClassroomId(Long classroomId, Pageable pageable);

    List<TimetablePeriod> findByTeacherId(Long teacherId, Pageable pageable);

    Optional<TimetablePeriod> findByIdAndSchoolId(Long id, Long schoolId);

    /** Same-day periods of a teacher / classroom, for the overlap check. */
    List<TimetablePeriod> findByTeacherIdAndDayOfWeek(Long teacherId, int dayOfWeek);

    List<TimetablePeriod> findByClassroomIdAndDayOfWeek(Long classroomId, int dayOfWeek);

    boolean existsByTeacherIdAndClassroomId(Long teacherId, Long classroomId);

    /** Distinct classrooms (with the subjects) a teacher lectures in. */
    List<TimetablePeriod> findByTeacherId(Long teacherId);

    long countByTeacherId(Long teacherId);

    /** (teacherId, periodCount) for a page of teachers - one aggregate query. */
    @Query("select p.teacherId, count(p.id) from TimetablePeriod p where p.teacherId in :ids group by p.teacherId")
    List<Object[]> countByTeacherIds(@Param("ids") Collection<Long> teacherIds);
}

package com.projectx.attendance.service;

import com.projectx.attendance.domain.ArrivalKind;
import com.projectx.attendance.domain.SchoolConfirm;
import com.projectx.attendance.dto.ArrivalDtos.ArrivalResponse;
import com.projectx.attendance.dto.TimetableDtos.TeacherAlert;
import com.projectx.common.security.CurrentUserContext;
import com.projectx.tracking.ws.StompRelayPublisher;
import java.sql.Date;
import java.time.Duration;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;

/**
 * C4: "the driver dropped this child at school but the register marks them ABSENT", for the child's class teacher.
 * The REST list ({@link #listMine}) and the live STOMP push ({@link #onArrivalChanged}, topic
 * {@code /topic/teacher/{userId}/alerts}) are the same query and the same DTO. Guardian contact is included because the
 * only teacher who ever receives an alert is the class teacher of the child's class.
 */
@Service
public class TeacherAlertService {

    private static final Logger log = LoggerFactory.getLogger(TeacherAlertService.class);
    static final int MAX_ALERTS_LISTED = 200;
    private static final Duration DEDUP_TTL = Duration.ofHours(24);

    private static final String SELECT = """
            SELECT av.id AS arrival_id, s.id AS student_id, s.first_name, s.last_name, c.id AS classroom_id, c.name, c.section,
                   c.class_teacher_id, av.driver_event_at, av.arrival_date, d.first_name AS driver_first, d.last_name AS driver_last,
                   v.registration_number, s.guardian_name, s.guardian_phone
              FROM arrival_verifications av
              JOIN students s ON s.id = av.student_id
              JOIN classrooms c ON c.id = s.classroom_id
              LEFT JOIN trips t ON t.id = av.trip_id
              LEFT JOIN driver_profiles d ON d.id = t.driver_id
              LEFT JOIN vehicles v ON v.id = t.vehicle_id
             WHERE av.kind = 'SCHOOL_DROP' AND av.school_confirm = 'ABSENT'
            """;

    private final JdbcTemplate jdbc;
    private final StringRedisTemplate redis;
    private final StompRelayPublisher relayPublisher;

    public TeacherAlertService(JdbcTemplate jdbc, StringRedisTemplate redis, StompRelayPublisher relayPublisher) {
        this.jdbc = jdbc;
        this.redis = redis;
        this.relayPublisher = relayPublisher;
    }

    /** The current class teacher's alerts for a day (default today, India time), newest first, capped at 200. No class = empty list. */
    public List<TeacherAlert> listMine(LocalDate date) {
        Long teacherId = CurrentUserContext.get().userId();
        LocalDate day = date != null ? date : SchoolTime.today();
        return jdbc.query(SELECT + " AND c.class_teacher_id = ? AND av.arrival_date = ? ORDER BY av.driver_event_at DESC LIMIT " + MAX_ALERTS_LISTED,
                MAPPER, teacherId, Date.valueOf(day));
    }

    /**
     * Called (async, attendanceExecutor, after commit) on every arrival change. Pushes one alert per arrival the first time
     * it becomes "dropped but ABSENT"; the Redis marker is cleared when the school corrects the mark so a later re-flip
     * alerts again. Best-effort: a failure is logged, the REST list is always the source of truth.
     */
    public void onArrivalChanged(ArrivalResponse arrival) {
        try {
            if (arrival.kind() != ArrivalKind.SCHOOL_DROP || arrival.studentId() == null) {
                return;
            }
            String key = "teacher-alert:" + arrival.id();
            if (arrival.schoolConfirm() != SchoolConfirm.ABSENT) {
                redis.delete(key);
                return;
            }
            Boolean first = redis.opsForValue().setIfAbsent(key, "1", DEDUP_TTL);
            if (Boolean.FALSE.equals(first)) {
                return;
            }
            Optional<TeacherAlertRow> row = jdbc.query(SELECT + " AND av.id = ?", ROW_MAPPER, arrival.id()).stream().findFirst();
            if (row.isEmpty() || row.get().teacherId() == null) {
                redis.delete(key);
                return;
            }
            relayPublisher.publish("/topic/teacher/" + row.get().teacherId() + "/alerts", row.get().alert());
        } catch (Exception e) {
            log.warn("Could not push teacher alert for arrival {}", arrival.id(), e);
        }
    }

    private record TeacherAlertRow(Long teacherId, TeacherAlert alert) {
    }

    private static final RowMapper<TeacherAlertRow> ROW_MAPPER = (rs, i) -> {
        String section = rs.getString("section");
        String className = section == null || section.isBlank() ? rs.getString("name") : rs.getString("name") + "-" + section;
        String driverFirst = rs.getString("driver_first");
        String driverName = driverFirst == null ? null : (driverFirst + " " + nullToEmpty(rs.getString("driver_last"))).trim();
        long teacher = rs.getLong("class_teacher_id");
        Long teacherId = rs.wasNull() ? null : teacher;
        return new TeacherAlertRow(teacherId, new TeacherAlert(rs.getLong("arrival_id"), rs.getLong("student_id"),
                (rs.getString("first_name") + " " + rs.getString("last_name")).trim(), rs.getLong("classroom_id"), className,
                rs.getTimestamp("driver_event_at").toInstant(), rs.getDate("arrival_date").toLocalDate(), driverName,
                rs.getString("registration_number"), rs.getString("guardian_name"), rs.getString("guardian_phone")));
    };

    private static final RowMapper<TeacherAlert> MAPPER = (rs, i) -> ROW_MAPPER.mapRow(rs, i).alert();

    private static String nullToEmpty(String s) {
        return s == null ? "" : s;
    }
}

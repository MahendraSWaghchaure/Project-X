package com.projectx.app;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.projectx.attendance.domain.ArrivalKind;
import com.projectx.attendance.domain.AttendanceStatus;
import com.projectx.attendance.dto.AcademicDtos.AssessmentRequest;
import com.projectx.attendance.dto.AcademicDtos.AssessmentResponse;
import com.projectx.attendance.dto.AcademicDtos.CommentRequest;
import com.projectx.attendance.dto.AcademicDtos.CommentResponse;
import com.projectx.attendance.dto.ArrivalDtos.ArrivalResponse;
import com.projectx.attendance.dto.AttendanceDtos.SheetRow;
import com.projectx.attendance.dto.StaffDtos.InviteTeacherRequest;
import com.projectx.attendance.dto.StaffDtos.InviteTeacherResponse;
import com.projectx.attendance.dto.StudentDtos.StudentResponse;
import com.projectx.attendance.dto.TimetableDtos.PeriodRequest;
import com.projectx.attendance.dto.TimetableDtos.PeriodResponse;
import com.projectx.attendance.dto.TimetableDtos.TeacherAlert;
import com.projectx.attendance.service.SchoolTime;
import com.projectx.common.events.ChildTripEventType;
import com.projectx.common.web.PageResponse;
import com.projectx.identity.dto.AcceptInvitationRequest;
import com.projectx.identity.dto.TokenResponse;
import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;

/**
 * C3/C4: what a CLASS_TEACHER, a LECTURER and an unrelated teacher may do, enforced server-side, and the "dropped but marked
 * absent" alert for the class teacher (REST list + live STOMP topic).
 */
class Phase2TeacherScopeTest extends SchoolAttendanceTestKit {

    private static final ObjectMapper JSON = new ObjectMapper();

    private record Teachers(String classTeacher, String lecturer, String unrelated, Long lecturerId) {}

    private String newTeacher(Env env, String prefix) {
        InviteTeacherResponse invite = ok(this.<InviteTeacherResponse>exchange("/schools/" + env.schoolId() + "/teachers", HttpMethod.POST,
                new InviteTeacherRequest(unique(prefix) + "@test.example", null, prefix + " Teacher", null, null), env.adminToken(),
                new ParameterizedTypeReference<>() {}));
        return ok(this.<TokenResponse>exchange("/auth/invitations/" + invite.inviteToken() + "/accept", HttpMethod.POST,
                new AcceptInvitationRequest("password123"), null, new ParameterizedTypeReference<>() {})).accessToken();
    }

    /** The kit's teacher is the class teacher of the kit classroom; add a lecturer (has a period there) and an unrelated teacher. */
    private Teachers teachers(Env env) {
        String lecturer = newTeacher(env, "lecturer");
        String unrelated = newTeacher(env, "unrelated");
        ok(this.<PeriodResponse>exchange("/schools/" + env.schoolId() + "/classrooms/" + env.classroomId() + "/periods", HttpMethod.POST,
                new PeriodRequest(jwtSubject(lecturer), "Mathematics", 2, LocalTime.of(9, 0), LocalTime.of(9, 45)), env.adminToken(),
                new ParameterizedTypeReference<>() {}));
        return new Teachers(env.teacherToken(), lecturer, unrelated, jwtSubject(lecturer));
    }

    @Test
    void teacherScopingMatrix() {
        Env env = buildEnv();
        Teachers t = teachers(env);
        String s = "/schools/" + env.schoolId();
        StudentResponse student = addStudent(env, "Scoped");

        // --- student detail: class teacher sees guardian contacts, lecturer does not, unrelated is refused.
        StudentResponse asClassTeacher = ok(this.<StudentResponse>exchange(s + "/students/" + student.id(), HttpMethod.GET, null, t.classTeacher(),
                new ParameterizedTypeReference<>() {}));
        assertThat(asClassTeacher.guardianName()).isEqualTo("Guardian Scoped");
        assertThat(asClassTeacher.guardianPhone()).isEqualTo(PHONE);
        StudentResponse asLecturer = ok(this.<StudentResponse>exchange(s + "/students/" + student.id(), HttpMethod.GET, null, t.lecturer(),
                new ParameterizedTypeReference<>() {}));
        assertThat(asLecturer.firstName()).isEqualTo("Scoped");
        assertThat(asLecturer.guardianName()).isNull();
        assertThat(asLecturer.guardianPhone()).isNull();
        assertThat(asLecturer.guardianEmail()).isNull();
        assertThat(asLecturer.consentStatus()).isNull();
        assertStatus(this.<StudentResponse>exchange(s + "/students/" + student.id(), HttpMethod.GET, null, t.unrelated(),
                new ParameterizedTypeReference<>() {}), HttpStatus.FORBIDDEN);

        // --- student list: scoped to the teacher's own classes; guardian contact only for the class teacher.
        String list = s + "/students?classroomId=" + env.classroomId();
        PageResponse<StudentResponse> ct = ok(this.<PageResponse<StudentResponse>>exchange(list, HttpMethod.GET, null, t.classTeacher(),
                new ParameterizedTypeReference<>() {}));
        assertThat(ct.content()).isNotEmpty().allMatch(x -> x.guardianPhone() != null);
        PageResponse<StudentResponse> lec = ok(this.<PageResponse<StudentResponse>>exchange(list, HttpMethod.GET, null, t.lecturer(),
                new ParameterizedTypeReference<>() {}));
        assertThat(lec.content()).isNotEmpty().allMatch(x -> x.guardianPhone() == null && x.guardianName() == null);
        assertStatus(this.<PageResponse<StudentResponse>>exchange(list, HttpMethod.GET, null, t.unrelated(), new ParameterizedTypeReference<>() {}),
                HttpStatus.FORBIDDEN);
        // Without a classroomId a teacher gets the union of their own classes - an unrelated teacher gets nothing, never the school.
        assertThat(ok(this.<PageResponse<StudentResponse>>exchange(s + "/students", HttpMethod.GET, null, t.unrelated(),
                new ParameterizedTypeReference<>() {})).content()).isEmpty();
        assertThat(ok(this.<PageResponse<StudentResponse>>exchange(s + "/students", HttpMethod.GET, null, t.lecturer(),
                new ParameterizedTypeReference<>() {})).content()).extracting(StudentResponse::id).contains(student.id());

        // --- attendance: class teacher only.
        String sheet = s + "/attendance?classroomId=" + env.classroomId();
        assertThat(ok(this.<List<SheetRow>>exchange(sheet, HttpMethod.GET, null, t.classTeacher(), new ParameterizedTypeReference<>() {})))
                .extracting(SheetRow::studentId).contains(student.id());
        assertStatus(this.<List<SheetRow>>exchange(sheet, HttpMethod.GET, null, t.lecturer(), new ParameterizedTypeReference<>() {}), HttpStatus.FORBIDDEN);
        assertStatus(this.<List<SheetRow>>exchange(sheet, HttpMethod.GET, null, t.unrelated(), new ParameterizedTypeReference<>() {}), HttpStatus.FORBIDDEN);
        assertStatus(this.<List<SheetRow>>exchange(s + "/attendance", HttpMethod.GET, null, t.classTeacher(), new ParameterizedTypeReference<>() {}),
                HttpStatus.BAD_REQUEST);
        assertSuccess(bulk(env, t.classTeacher(), SchoolTime.today(), student.id(), AttendanceStatus.PRESENT));
        assertStatus(bulk(env, t.lecturer(), SchoolTime.today(), student.id(), AttendanceStatus.ABSENT), HttpStatus.FORBIDDEN);
        assertStatus(bulk(env, t.unrelated(), SchoolTime.today(), student.id(), AttendanceStatus.ABSENT), HttpStatus.FORBIDDEN);

        // --- comments: class teacher only.
        String comments = s + "/students/" + student.id() + "/comments";
        assertStatus(this.<CommentResponse>exchange(comments, HttpMethod.POST, new CommentRequest("Well behaved"), t.classTeacher(),
                new ParameterizedTypeReference<>() {}), HttpStatus.CREATED);
        for (String denied : List.of(t.lecturer(), t.unrelated())) {
            assertStatus(this.<CommentResponse>exchange(comments, HttpMethod.POST, new CommentRequest("Sneaky"), denied,
                    new ParameterizedTypeReference<>() {}), HttpStatus.FORBIDDEN);
            assertStatus(this.<List<CommentResponse>>exchange(comments, HttpMethod.GET, null, denied, new ParameterizedTypeReference<>() {}),
                    HttpStatus.FORBIDDEN);
        }
        assertThat(ok(this.<List<CommentResponse>>exchange(comments, HttpMethod.GET, null, t.classTeacher(), new ParameterizedTypeReference<>() {})))
                .hasSize(1);

        // --- assessments: class teacher AND lecturer; unrelated refused.
        String assessments = s + "/students/" + student.id() + "/assessments";
        AssessmentRequest exam = new AssessmentRequest("Unit test 1", "Mathematics", new BigDecimal("50"), new BigDecimal("42.5"), SchoolTime.today());
        assertStatus(this.<AssessmentResponse>exchange(assessments, HttpMethod.POST, exam, t.classTeacher(), new ParameterizedTypeReference<>() {}),
                HttpStatus.CREATED);
        assertStatus(this.<AssessmentResponse>exchange(assessments, HttpMethod.POST, exam, t.lecturer(), new ParameterizedTypeReference<>() {}),
                HttpStatus.CREATED);
        assertStatus(this.<AssessmentResponse>exchange(assessments, HttpMethod.POST, exam, t.unrelated(), new ParameterizedTypeReference<>() {}),
                HttpStatus.FORBIDDEN);
        assertThat(ok(this.<List<AssessmentResponse>>exchange(assessments, HttpMethod.GET, null, t.lecturer(), new ParameterizedTypeReference<>() {})))
                .hasSize(2);
        assertStatus(this.<List<AssessmentResponse>>exchange(assessments, HttpMethod.GET, null, t.unrelated(), new ParameterizedTypeReference<>() {}),
                HttpStatus.FORBIDDEN);

        // --- the school-wide arrival board is not a teacher thing any more; nor is sending consent links or managing classes.
        for (String teacher : List.of(t.classTeacher(), t.lecturer(), t.unrelated())) {
            assertStatus(this.<Object>exchange(s + "/arrivals", HttpMethod.GET, null, teacher, new ParameterizedTypeReference<>() {}), HttpStatus.FORBIDDEN);
            assertStatus(this.<Object>exchange(s + "/students/" + student.id() + "/consent-link", HttpMethod.POST, null, teacher,
                    new ParameterizedTypeReference<>() {}), HttpStatus.FORBIDDEN);
            assertStatus(this.<Object>exchange(s + "/classrooms/" + env.classroomId() + "/class-teacher", HttpMethod.PUT, Map.of("teacherId", 1), teacher,
                    new ParameterizedTypeReference<>() {}), HttpStatus.FORBIDDEN);
        }
        // Classes: each teacher sees only theirs.
        assertThat(ok(this.<List<Object>>exchange(s + "/classrooms", HttpMethod.GET, null, t.unrelated(), new ParameterizedTypeReference<>() {}))).isEmpty();
        assertThat(ok(this.<List<Object>>exchange(s + "/classrooms", HttpMethod.GET, null, t.lecturer(), new ParameterizedTypeReference<>() {}))).hasSize(1);

        // --- a teacher of ANOTHER school is refused at the tenant check.
        Env other = buildEnv();
        assertStatus(this.<StudentResponse>exchange(s + "/students/" + student.id(), HttpMethod.GET, null, other.teacherToken(),
                new ParameterizedTypeReference<>() {}), HttpStatus.FORBIDDEN);
        // --- a student with no classroom is nobody's student: only admins see them.
        StudentResponse noClass = ok(this.<StudentResponse>exchange(s + "/students", HttpMethod.POST,
                Map.of("firstName", "Loose", "lastName", "Student", "guardianName", "G", "guardianPhone", PHONE), env.adminToken(),
                new ParameterizedTypeReference<>() {}));
        assertStatus(this.<StudentResponse>exchange(s + "/students/" + noClass.id(), HttpMethod.GET, null, t.classTeacher(),
                new ParameterizedTypeReference<>() {}), HttpStatus.FORBIDDEN);
        assertStatus(this.<StudentResponse>exchange(s + "/students/" + noClass.id(), HttpMethod.GET, null, env.adminToken(),
                new ParameterizedTypeReference<>() {}), HttpStatus.OK);
    }

    @Test
    void classTeacherGetsAnAlertWhenTheDriverDropsAChildTheSchoolMarksAbsent() throws Exception {
        Env env = buildEnv();
        Teachers t = teachers(env);
        Long child = env.childIds().get(0);
        StudentResponse student = subscribed(env, "Dropped", child);
        String alertsPath = "/teachers/me/alerts";

        // Before anything happens: nothing.
        assertThat(ok(this.<List<TeacherAlert>>exchange(alertsPath, HttpMethod.GET, null, t.classTeacher(), new ParameterizedTypeReference<>() {}))).isEmpty();

        assertSuccess(driverEvent(env, child, ChildTripEventType.DROPPED_AT_SCHOOL));
        ArrivalResponse arrival = awaitArrival(env, child, ArrivalKind.SCHOOL_DROP);

        try (StompProbe live = StompProbe.connect(port, t.classTeacher())) {
            assertThat(live.subscribe("/topic/teacher/" + jwtSubject(t.classTeacher()) + "/alerts")).as("own topic").isTrue();

            // Marked PRESENT: no alert.
            assertSuccess(bulk(env, t.classTeacher(), SchoolTime.today(), student.id(), AttendanceStatus.PRESENT));
            assertThat(ok(this.<List<TeacherAlert>>exchange(alertsPath, HttpMethod.GET, null, t.classTeacher(), new ParameterizedTypeReference<>() {})))
                    .isEmpty();
            assertThat(live.poll(1)).as("no frame for a present child").isNull();

            // Marked ABSENT after the drop: alert (REST + live).
            assertSuccess(bulk(env, t.classTeacher(), SchoolTime.today(), student.id(), AttendanceStatus.ABSENT));
            String frame = live.poll(10);
            assertThat(frame).as("live alert frame").isNotNull();
            JsonNode node = JSON.readTree(frame);
            assertThat(node.get("arrivalId").asLong()).isEqualTo(arrival.id());
            assertThat(node.get("studentName").asText()).isEqualTo("Dropped Student");
            assertThat(node.get("droppedAt").isTextual()).isTrue();
            assertThat(node.get("guardianPhone").asText()).isEqualTo(PHONE);

            List<TeacherAlert> alerts = ok(this.<List<TeacherAlert>>exchange(alertsPath, HttpMethod.GET, null, t.classTeacher(),
                    new ParameterizedTypeReference<>() {}));
            assertThat(alerts).singleElement().satisfies(a -> {
                assertThat(a.arrivalId()).isEqualTo(arrival.id());
                assertThat(a.studentId()).isEqualTo(student.id());
                assertThat(a.classroomId()).isEqualTo(env.classroomId());
                assertThat(a.classroomName()).isNotBlank();
                assertThat(a.driverName()).isEqualTo("Driver Test");
                assertThat(a.vehicleNo()).isNotBlank();
                assertThat(a.guardianName()).isEqualTo("Guardian Dropped");
                assertThat(a.date()).isEqualTo(SchoolTime.today());
            });
            // The lecturer and the unrelated teacher hear nothing, and yesterday has no alerts.
            assertThat(ok(this.<List<TeacherAlert>>exchange(alertsPath, HttpMethod.GET, null, t.lecturer(), new ParameterizedTypeReference<>() {}))).isEmpty();
            assertThat(ok(this.<List<TeacherAlert>>exchange(alertsPath, HttpMethod.GET, null, t.unrelated(), new ParameterizedTypeReference<>() {}))).isEmpty();
            assertThat(ok(this.<List<TeacherAlert>>exchange(alertsPath + "?date=" + SchoolTime.today().minusDays(1), HttpMethod.GET, null,
                    t.classTeacher(), new ParameterizedTypeReference<>() {}))).isEmpty();

            // Correcting the register clears the alert; flipping to ABSENT again alerts again (the dedupe marker was cleared).
            assertSuccess(bulk(env, t.classTeacher(), SchoolTime.today(), student.id(), AttendanceStatus.LATE));
            assertThat(ok(this.<List<TeacherAlert>>exchange(alertsPath, HttpMethod.GET, null, t.classTeacher(), new ParameterizedTypeReference<>() {}))).isEmpty();
            live.frames.clear();
            assertSuccess(bulk(env, t.classTeacher(), SchoolTime.today(), student.id(), AttendanceStatus.ABSENT));
            assertThat(live.poll(10)).as("second alert after a correction").isNotNull();
        }

        // Only teachers have the endpoint, and a stranger cannot listen to someone else's topic.
        assertStatus(this.<Object>exchange(alertsPath, HttpMethod.GET, null, env.adminToken(), new ParameterizedTypeReference<>() {}), HttpStatus.FORBIDDEN);
        assertStatus(this.<Object>exchange(alertsPath, HttpMethod.GET, null, env.parentToken(), new ParameterizedTypeReference<>() {}), HttpStatus.FORBIDDEN);
        String classTeacherTopic = "/topic/teacher/" + jwtSubject(t.classTeacher()) + "/alerts";
        for (String stranger : List.of(t.lecturer(), t.unrelated(), env.adminToken(), env.parentToken(), env.driverToken(), platformAdminToken())) {
            try (StompProbe probe = StompProbe.connect(port, stranger)) {
                assertThat(probe.subscribe(classTeacherTopic)).isFalse();
            }
        }
        for (String bad : List.of("/topic/teacher/" + t.lecturerId() + "/alerts", "/topic/teacher/*/alerts", "/topic/teacher/**")) {
            // one connection per attempt: a rejected SUBSCRIBE makes the server close the socket
            try (StompProbe probe = StompProbe.connect(port, t.classTeacher())) {
                assertThat(probe.subscribe(bad)).as(bad).isFalse();
            }
        }
    }
}

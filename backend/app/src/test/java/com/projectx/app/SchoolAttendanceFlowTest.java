package com.projectx.app;

import static org.assertj.core.api.Assertions.assertThat;

import com.projectx.attendance.domain.ArrivalKind;
import com.projectx.attendance.domain.ArrivalOutcome;
import com.projectx.attendance.domain.AttendanceStatus;
import com.projectx.attendance.domain.ParentResponse;
import com.projectx.attendance.domain.SchoolConfirm;
import com.projectx.attendance.domain.Subscription;
import com.projectx.attendance.dto.AcademicDtos.AssessmentRequest;
import com.projectx.attendance.dto.AcademicDtos.AssessmentResponse;
import com.projectx.attendance.dto.AcademicDtos.CommentRequest;
import com.projectx.attendance.dto.AcademicDtos.CommentResponse;
import com.projectx.attendance.dto.ArrivalDtos.ArrivalResponse;
import com.projectx.attendance.dto.ArrivalDtos.RespondRequest;
import com.projectx.attendance.dto.AttendanceDtos.AttendanceSummary;
import com.projectx.attendance.dto.AttendanceDtos.BulkAttendanceRequest;
import com.projectx.attendance.dto.AttendanceDtos.BulkAttendanceResult;
import com.projectx.attendance.dto.AttendanceDtos.BulkEntry;
import com.projectx.attendance.dto.AttendanceDtos.SheetRow;
import com.projectx.attendance.dto.ClassroomDtos.ClassroomRequest;
import com.projectx.attendance.dto.ClassroomDtos.ClassroomResponse;
import com.projectx.attendance.dto.StaffDtos.InviteTeacherRequest;
import com.projectx.attendance.dto.StaffDtos.InviteTeacherResponse;
import com.projectx.attendance.dto.StaffDtos.LeadResponse;
import com.projectx.attendance.dto.StudentDtos.ImportResult;
import com.projectx.attendance.dto.StudentDtos.LinkChildRequest;
import com.projectx.attendance.dto.StudentDtos.StudentRequest;
import com.projectx.attendance.dto.StudentDtos.StudentResponse;
import com.projectx.attendance.service.SchoolTime;
import com.projectx.common.events.ChildTripEventType;
import com.projectx.common.web.ApiResponse;
import com.projectx.common.web.PageResponse;
import com.projectx.identity.dto.AcceptInvitationRequest;
import com.projectx.identity.dto.TokenResponse;
import com.projectx.people.dto.ChildResponse;
import com.projectx.people.dto.CreateChildRequest;
import com.projectx.people.dto.CreateDriverRequest;
import com.projectx.people.dto.DriverResponse;
import com.projectx.people.dto.InviteParentRequest;
import com.projectx.people.dto.InviteParentResponse;
import com.projectx.school.domain.VehicleType;
import com.projectx.school.dto.AssignRouteRequest;
import com.projectx.school.dto.CreateRouteRequest;
import com.projectx.school.dto.CreateSchoolRequest;
import com.projectx.school.dto.CreateVehicleRequest;
import com.projectx.school.dto.RouteResponse;
import com.projectx.school.dto.RouteStopRequest;
import com.projectx.school.dto.SchoolResponse;
import com.projectx.school.dto.VehicleResponse;
import com.projectx.tracking.domain.TripDirection;
import com.projectx.tracking.dto.ChildTripEventResponse;
import com.projectx.tracking.dto.CreateTripRequest;
import com.projectx.tracking.dto.RecordChildTripEventRequest;
import com.projectx.tracking.dto.TripResponse;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

/**
 * End-to-end coverage of the school attendance / safe-arrival feature over real HTTP against the
 * migrated schema (same style as SosAbsenceAndQrScanTest): bulk idempotency, tenant isolation,
 * arrival outcome transitions, subscribed vs non-subscribed students, and the leads consent filter.
 * The arrival row is opened asynchronously (attendanceExecutor) after the driver's request commits,
 * so tests poll for it instead of assuming it exists immediately.
 */
class SchoolAttendanceFlowTest extends IntegrationTestBase {

    private static final String PHONE = "9876543210";

    /** Everything one school needs: staff tokens, a classroom, a parent with three children, a started morning trip. */
    private record Env(Long schoolId, String adminToken, String teacherToken, String driverToken, String parentToken,
                       Long classroomId, Long tripId, List<Long> childIds) {}

    private <T> T ok(ResponseEntity<ApiResponse<T>> response) {
        assertSuccess(response);
        return response.getBody().data();
    }

    private Long createSchool(String platformToken, String adminEmail) {
        var request = new CreateSchoolRequest(unique("Att School "), "1 Test Rd", "Pune", null, null,
                null, null, null, 18.5204, 73.8567, 300, null, "Admin", "User", adminEmail, "password123");
        return ok(this.<SchoolResponse>exchange("/schools", HttpMethod.POST, request, platformToken,
                new ParameterizedTypeReference<>() {})).id();
    }

    private Env buildEnv() {
        String platformToken = platformAdminToken();
        String adminEmail = unique("schooladmin") + "@test.example";
        Long schoolId = createSchool(platformToken, adminEmail);
        String adminToken = login(adminEmail, "password123");

        // Teacher: invited by the school admin, accepts the invitation, gets a school-bound token.
        String teacherEmail = unique("teacher") + "@test.example";
        InviteTeacherResponse invite = ok(this.<InviteTeacherResponse>exchange("/schools/" + schoolId + "/teachers",
                HttpMethod.POST, new InviteTeacherRequest(teacherEmail, null), adminToken, new ParameterizedTypeReference<>() {}));
        String teacherToken = ok(this.<TokenResponse>exchange("/auth/invitations/" + invite.inviteToken() + "/accept",
                HttpMethod.POST, new AcceptInvitationRequest("password123"), null, new ParameterizedTypeReference<>() {})).accessToken();

        Long classroomId = ok(this.<ClassroomResponse>exchange("/schools/" + schoolId + "/classrooms", HttpMethod.POST,
                new ClassroomRequest("Class " + unique(""), "A"), adminToken, new ParameterizedTypeReference<>() {})).id();
        makeClassTeacher(schoolId, classroomId, adminToken, teacherToken);

        Long routeId = ok(this.<RouteResponse>exchange("/schools/" + schoolId + "/routes", HttpMethod.POST,
                new CreateRouteRequest(unique("Route "), List.of(new RouteStopRequest(1, "Main Gate", 18.53, 73.86, 150)), 0, 6000),
                adminToken, new ParameterizedTypeReference<>() {})).id();
        Long vehicleId = ok(this.<VehicleResponse>exchange("/schools/" + schoolId + "/vehicles", HttpMethod.POST,
                new CreateVehicleRequest(unique("REG").toUpperCase(), VehicleType.VAN, 12, null), adminToken,
                new ParameterizedTypeReference<>() {})).id();
        String driverEmail = unique("driver") + "@test.example";
        Long driverId = ok(this.<DriverResponse>exchange("/schools/" + schoolId + "/drivers", HttpMethod.POST,
                new CreateDriverRequest("Driver", "Test", driverEmail, "password123", "9900000001", unique("LIC"), null, null),
                adminToken, new ParameterizedTypeReference<>() {})).id();
        String driverToken = login(driverEmail, "password123");
        ok(this.<RouteResponse>exchange("/schools/" + schoolId + "/routes/" + routeId + "/assignment", HttpMethod.PUT,
                new AssignRouteRequest(vehicleId, driverId), adminToken, new ParameterizedTypeReference<>() {}));

        String parentToken = newParent(schoolId, adminToken);
        List<Long> childIds = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            childIds.add(ok(this.<ChildResponse>exchange("/parents/me/children", HttpMethod.POST,
                    new CreateChildRequest("Kid" + i, "Test", null, "3A", null, null, null, null, null), parentToken,
                    new ParameterizedTypeReference<>() {})).id());
        }

        Long tripId = ok(this.<TripResponse>exchange("/schools/" + schoolId + "/trips", HttpMethod.POST,
                new CreateTripRequest(routeId, TripDirection.MORNING_PICKUP, SchoolTime.today()), adminToken,
                new ParameterizedTypeReference<>() {})).id();
        ok(this.<TripResponse>exchange("/trips/" + tripId + "/start", HttpMethod.POST, null, driverToken,
                new ParameterizedTypeReference<>() {}));
        return new Env(schoolId, adminToken, teacherToken, driverToken, parentToken, classroomId, tripId, childIds);
    }

    private String newParent(Long schoolId, String adminToken) {
        InviteParentResponse invite = ok(this.<InviteParentResponse>exchange("/schools/" + schoolId + "/parents/invitations",
                HttpMethod.POST, new InviteParentRequest(unique("parent") + "@test.example", null), adminToken,
                new ParameterizedTypeReference<>() {}));
        return ok(this.<TokenResponse>exchange("/auth/invitations/" + invite.inviteToken() + "/accept", HttpMethod.POST,
                new AcceptInvitationRequest("password123"), null, new ParameterizedTypeReference<>() {})).accessToken();
    }

    private StudentResponse addStudent(Env env, String first, boolean consent, String leadSource) {
        return addStudent(env.schoolId(), env.adminToken(), env.classroomId(), first, consent, leadSource);
    }

    private StudentResponse addStudent(Long schoolId, String token, Long classroomId, String first, boolean consent, String leadSource) {
        var request = new StudentRequest(classroomId, null, first, "Student", null, null, "Guardian " + first, PHONE, null,
                consent, leadSource, null);
        return ok(this.<StudentResponse>exchange("/schools/" + schoolId + "/students", HttpMethod.POST, request, token,
                new ParameterizedTypeReference<>() {}));
    }

    private StudentResponse subscribedStudent(Env env, String first, Long childId) {
        StudentResponse student = addStudent(env, first, false, null);
        return ok(this.<StudentResponse>exchange("/schools/" + env.schoolId() + "/students/" + student.id() + "/link-child",
                HttpMethod.POST, new LinkChildRequest(childId), env.adminToken(), new ParameterizedTypeReference<>() {}));
    }

    private BulkAttendanceRequest sheet(Env env, LocalDate date, Object... idAndStatus) {
        List<BulkEntry> entries = new ArrayList<>();
        for (int i = 0; i < idAndStatus.length; i += 2) {
            entries.add(new BulkEntry((Long) idAndStatus[i], (AttendanceStatus) idAndStatus[i + 1]));
        }
        return new BulkAttendanceRequest(date, env.classroomId(), entries);
    }

    private ResponseEntity<ApiResponse<BulkAttendanceResult>> putBulk(Long schoolId, String token, BulkAttendanceRequest req) {
        return exchange("/schools/" + schoolId + "/attendance/bulk", HttpMethod.PUT, req, token, new ParameterizedTypeReference<>() {});
    }

    private List<SheetRow> readSheet(Env env, LocalDate date) {
        return ok(this.<List<SheetRow>>exchange("/schools/" + env.schoolId() + "/attendance?date=" + date + "&classroomId=" + env.classroomId(),
                HttpMethod.GET, null, env.teacherToken(), new ParameterizedTypeReference<>() {}));
    }

    private void driverRecords(Env env, Long childId, ChildTripEventType type) {
        ok(this.<ChildTripEventResponse>exchange("/trips/" + env.tripId() + "/child-events", HttpMethod.POST,
                new RecordChildTripEventRequest(childId, type), env.driverToken(), new ParameterizedTypeReference<>() {}));
    }

    /** The arrival is opened off the request thread - poll the driver's own endpoint until it shows up (max ~10s). */
    private ArrivalResponse awaitArrival(Env env, Long childId, ArrivalKind kind) {
        for (int i = 0; i < 50; i++) {
            List<ArrivalResponse> arrivals = ok(this.<List<ArrivalResponse>>exchange("/trips/" + env.tripId() + "/arrivals",
                    HttpMethod.GET, null, env.driverToken(), new ParameterizedTypeReference<>() {}));
            var match = arrivals.stream().filter(a -> a.childId().equals(childId) && a.kind() == kind).findFirst();
            if (match.isPresent()) {
                return match.get();
            }
            sleep(200);
        }
        throw new AssertionError("Arrival for child " + childId + " kind " + kind + " never appeared");
    }

    private static void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    // ------------------------------------------------------------------------------------------

    @Test
    void bulkAttendanceIsIdempotentReportsEditsAndEnforcesCaps() {
        Env env = buildEnv();
        StudentResponse s1 = addStudent(env, "Asha", false, null);
        StudentResponse s2 = addStudent(env, "Bela", false, null);
        StudentResponse s3 = addStudent(env, "Chitra", false, null);
        LocalDate today = SchoolTime.today();

        // Teacher marks the class.
        BulkAttendanceResult first = ok(putBulk(env.schoolId(), env.teacherToken(),
                sheet(env, today, s1.id(), AttendanceStatus.PRESENT, s2.id(), AttendanceStatus.ABSENT, s3.id(), AttendanceStatus.LATE)));
        assertThat(first.inserted()).isEqualTo(3);
        assertThat(first.updated()).isZero();

        // Re-sending the identical sheet is a no-op write.
        BulkAttendanceResult replay = ok(putBulk(env.schoolId(), env.teacherToken(),
                sheet(env, today, s1.id(), AttendanceStatus.PRESENT, s2.id(), AttendanceStatus.ABSENT, s3.id(), AttendanceStatus.LATE)));
        assertThat(replay.inserted()).isZero();
        assertThat(replay.updated()).isZero();
        assertThat(replay.unchanged()).isEqualTo(3);

        // Correcting one mark is reported as an edit, not an insert, and leaves exactly one row per student/day.
        BulkAttendanceResult edit = ok(putBulk(env.schoolId(), env.adminToken(),
                sheet(env, today, s2.id(), AttendanceStatus.PRESENT)));
        assertThat(edit.inserted()).isZero();
        assertThat(edit.updated()).isEqualTo(1);

        List<SheetRow> rows = readSheet(env, today);
        assertThat(rows).hasSize(3);
        assertThat(rows).filteredOn(r -> r.studentId().equals(s2.id())).singleElement()
                .extracting(SheetRow::status).isEqualTo(AttendanceStatus.PRESENT);
        assertThat(rows).filteredOn(r -> r.studentId().equals(s3.id())).singleElement()
                .extracting(SheetRow::status).isEqualTo(AttendanceStatus.LATE);

        AttendanceSummary summary = ok(this.<AttendanceSummary>exchange(
                "/schools/" + env.schoolId() + "/attendance/summary?date=" + today, HttpMethod.GET, null, env.adminToken(),
                new ParameterizedTypeReference<>() {}));
        assertThat(summary.total()).isEqualTo(3);
        assertThat(summary.present()).isEqualTo(2);
        assertThat(summary.absent()).isZero();
        assertThat(summary.late()).isEqualTo(1);
        assertThat(summary.unmarked()).isZero();
        assertThat(summary.nonSubscribed()).isEqualTo(3);
        assertThat(summary.subscribed()).isZero();

        // Guards: duplicate student, future date, unknown classroom, and the 500-row cap.
        assertStatus(putBulk(env.schoolId(), env.teacherToken(),
                sheet(env, today, s1.id(), AttendanceStatus.PRESENT, s1.id(), AttendanceStatus.ABSENT)), HttpStatus.BAD_REQUEST);
        assertStatus(putBulk(env.schoolId(), env.teacherToken(),
                sheet(env, today.plusDays(2), s1.id(), AttendanceStatus.PRESENT)), HttpStatus.BAD_REQUEST);
        List<BulkEntry> tooMany = IntStream.rangeClosed(1, 501).mapToObj(i -> new BulkEntry((long) i, AttendanceStatus.PRESENT)).toList();
        assertStatus(putBulk(env.schoolId(), env.teacherToken(), new BulkAttendanceRequest(today, env.classroomId(), tooMany)),
                HttpStatus.BAD_REQUEST);
        assertStatus(putBulk(env.schoolId(), env.teacherToken(),
                new BulkAttendanceRequest(today, 987654321L, List.of(new BulkEntry(s1.id(), AttendanceStatus.PRESENT)))),
                HttpStatus.NOT_FOUND);

        // Student list is size-capped at 100 no matter what is asked for.
        PageResponse<StudentResponse> page = ok(this.<PageResponse<StudentResponse>>exchange(
                "/schools/" + env.schoolId() + "/students?size=100000", HttpMethod.GET, null, env.teacherToken(),
                new ParameterizedTypeReference<>() {}));
        assertThat(page.size()).isEqualTo(100);
        assertThat(page.totalElements()).isEqualTo(3);
    }

    @Test
    void staleStudentVersionIsRejectedAndImportIsIdempotent() {
        Env env = buildEnv();
        StudentResponse student = addStudent(env, "Dev", false, null);
        var update = new StudentRequest(env.classroomId(), "R1", "Dev", "Renamed", null, null, "Guardian", PHONE, null, false, null, student.version());
        StudentResponse updated = ok(this.<StudentResponse>exchange("/schools/" + env.schoolId() + "/students/" + student.id(),
                HttpMethod.PUT, update, env.adminToken(), new ParameterizedTypeReference<>() {}));
        assertThat(updated.lastName()).isEqualTo("Renamed");
        assertThat(updated.version()).isGreaterThan(student.version());

        // Same (now stale) version again -> 409 instead of silently overwriting.
        assertStatus(this.<StudentResponse>exchange("/schools/" + env.schoolId() + "/students/" + student.id(), HttpMethod.PUT,
                update, env.adminToken(), new ParameterizedTypeReference<>() {}), HttpStatus.CONFLICT);

        // Import: first upload creates, identical re-upload updates in place (no duplicates); >500 rows rejected.
        List<StudentRequest> rows = List.of(
                new StudentRequest(env.classroomId(), "I1", "Imp", "One", null, null, "G1", PHONE, null, false, "IMPORT", null),
                new StudentRequest(env.classroomId(), "I2", "Imp", "Two", null, null, "G2", PHONE, null, null, "IMPORT", null));
        var body = java.util.Map.of("rows", rows);
        ImportResult first = ok(this.<ImportResult>exchange("/schools/" + env.schoolId() + "/students/import", HttpMethod.POST,
                body, env.adminToken(), new ParameterizedTypeReference<>() {}));
        assertThat(first.created()).isEqualTo(2);
        ImportResult second = ok(this.<ImportResult>exchange("/schools/" + env.schoolId() + "/students/import", HttpMethod.POST,
                body, env.adminToken(), new ParameterizedTypeReference<>() {}));
        assertThat(second.created()).isZero();
        assertThat(second.updated()).isEqualTo(2);

        List<StudentRequest> tooMany = IntStream.range(0, 501)
                .mapToObj(i -> new StudentRequest(null, "X" + i, "A", "B", null, null, "G", PHONE, null, false, null, null)).toList();
        assertStatus(this.<ImportResult>exchange("/schools/" + env.schoolId() + "/students/import", HttpMethod.POST,
                java.util.Map.of("rows", tooMany), env.adminToken(), new ParameterizedTypeReference<>() {}), HttpStatus.BAD_REQUEST);
    }

    @Test
    void schoolDataIsTenantIsolatedAndRolesAreEnforced() {
        Env a = buildEnv();
        String platformToken = platformAdminToken();
        String bAdminEmail = unique("bschooladmin") + "@test.example";
        Long schoolB = createSchool(platformToken, bAdminEmail);
        String bAdminToken = login(bAdminEmail, "password123");
        Long classroomB = ok(this.<ClassroomResponse>exchange("/schools/" + schoolB + "/classrooms", HttpMethod.POST,
                new ClassroomRequest("B-Class " + unique(""), null), bAdminToken, new ParameterizedTypeReference<>() {})).id();
        StudentResponse studentA = addStudent(a, "Alpha", false, null);
        StudentResponse studentB = addStudent(schoolB, bAdminToken, classroomB, "Beta", false, null);

        // School B's admin cannot read or write School A's data through School A's paths.
        String aBase = "/schools/" + a.schoolId();
        assertStatus(this.<List<ClassroomResponse>>exchange(aBase + "/classrooms", HttpMethod.GET, null, bAdminToken,
                new ParameterizedTypeReference<>() {}), HttpStatus.FORBIDDEN);
        assertStatus(this.<PageResponse<StudentResponse>>exchange(aBase + "/students", HttpMethod.GET, null, bAdminToken,
                new ParameterizedTypeReference<>() {}), HttpStatus.FORBIDDEN);
        assertStatus(this.<AttendanceSummary>exchange(aBase + "/attendance/summary", HttpMethod.GET, null, bAdminToken,
                new ParameterizedTypeReference<>() {}), HttpStatus.FORBIDDEN);
        assertStatus(this.<PageResponse<ArrivalResponse>>exchange(aBase + "/arrivals", HttpMethod.GET, null, bAdminToken,
                new ParameterizedTypeReference<>() {}), HttpStatus.FORBIDDEN);
        assertStatus(this.<List<AssessmentResponse>>exchange(aBase + "/students/" + studentA.id() + "/assessments", HttpMethod.GET,
                null, bAdminToken, new ParameterizedTypeReference<>() {}), HttpStatus.FORBIDDEN);

        // Cross-tenant ids smuggled into the caller's OWN school paths are 404 / 400, never data.
        assertStatus(this.<StudentResponse>exchange("/schools/" + schoolB + "/students/" + studentA.id(), HttpMethod.GET, null,
                bAdminToken, new ParameterizedTypeReference<>() {}), HttpStatus.NOT_FOUND);
        assertStatus(this.<CommentResponse>exchange("/schools/" + schoolB + "/students/" + studentA.id() + "/comments",
                HttpMethod.POST, new CommentRequest("hi"), bAdminToken, new ParameterizedTypeReference<>() {}), HttpStatus.NOT_FOUND);
        assertStatus(putBulk(schoolB, bAdminToken, new BulkAttendanceRequest(SchoolTime.today(), a.classroomId(),
                List.of(new BulkEntry(studentA.id(), AttendanceStatus.PRESENT)))), HttpStatus.NOT_FOUND);
        assertStatus(putBulk(schoolB, bAdminToken, new BulkAttendanceRequest(SchoolTime.today(), classroomB,
                List.of(new BulkEntry(studentA.id(), AttendanceStatus.PRESENT)))), HttpStatus.BAD_REQUEST);
        assertThat(readSheetRaw(schoolB, bAdminToken, classroomB)).extracting(SheetRow::studentId)
                .containsExactly(studentB.id()).doesNotContain(studentA.id());

        // A teacher of school A: can work in school A, not in B, and cannot manage the roster or teachers.
        assertStatus(this.<List<SheetRow>>exchange("/schools/" + schoolB + "/attendance", HttpMethod.GET, null, a.teacherToken(),
                new ParameterizedTypeReference<>() {}), HttpStatus.FORBIDDEN);
        assertStatus(this.<ClassroomResponse>exchange(aBase + "/classrooms", HttpMethod.POST, new ClassroomRequest("Nope", null),
                a.teacherToken(), new ParameterizedTypeReference<>() {}), HttpStatus.FORBIDDEN);
        assertStatus(this.<StudentResponse>exchange(aBase + "/students", HttpMethod.POST,
                new StudentRequest(a.classroomId(), null, "T", "T", null, null, "G", PHONE, null, false, null, null),
                a.teacherToken(), new ParameterizedTypeReference<>() {}), HttpStatus.FORBIDDEN);
        assertStatus(this.<InviteTeacherResponse>exchange(aBase + "/teachers", HttpMethod.POST,
                new InviteTeacherRequest(unique("x") + "@test.example", null), a.teacherToken(), new ParameterizedTypeReference<>() {}),
                HttpStatus.FORBIDDEN);
        CommentResponse comment = ok(this.<CommentResponse>exchange(aBase + "/students/" + studentA.id() + "/comments", HttpMethod.POST,
                new CommentRequest("Participates well"), a.teacherToken(), new ParameterizedTypeReference<>() {}));
        assertThat(comment.text()).isEqualTo("Participates well");
        AssessmentResponse assessment = ok(this.<AssessmentResponse>exchange(aBase + "/students/" + studentA.id() + "/assessments",
                HttpMethod.POST, new AssessmentRequest("Unit Test 1", "Maths", new BigDecimal("50"), new BigDecimal("42.5"),
                        SchoolTime.today()), a.teacherToken(), new ParameterizedTypeReference<>() {}));
        assertThat(assessment.marksObtained()).isEqualByComparingTo("42.5");
        assertStatus(this.<AssessmentResponse>exchange(aBase + "/students/" + studentA.id() + "/assessments", HttpMethod.POST,
                new AssessmentRequest("Bad", "Maths", new BigDecimal("50"), new BigDecimal("60"), SchoolTime.today()),
                a.teacherToken(), new ParameterizedTypeReference<>() {}), HttpStatus.BAD_REQUEST);
        assertThat(ok(this.<List<CommentResponse>>exchange(aBase + "/students/" + studentA.id() + "/comments", HttpMethod.GET, null,
                a.adminToken(), new ParameterizedTypeReference<>() {}))).hasSize(1);

        // Parents and drivers have no business on the school sheet.
        assertStatus(this.<AttendanceSummary>exchange(aBase + "/attendance/summary", HttpMethod.GET, null, a.parentToken(),
                new ParameterizedTypeReference<>() {}), HttpStatus.FORBIDDEN);
        assertStatus(this.<AttendanceSummary>exchange(aBase + "/attendance/summary", HttpMethod.GET, null, a.driverToken(),
                new ParameterizedTypeReference<>() {}), HttpStatus.FORBIDDEN);

        // Linking a child of another school is a 404 (never confirms the child exists).
        assertStatus(this.<StudentResponse>exchange("/schools/" + schoolB + "/students/" + studentB.id() + "/link-child",
                HttpMethod.POST, new LinkChildRequest(a.childIds().get(0)), bAdminToken, new ParameterizedTypeReference<>() {}),
                HttpStatus.NOT_FOUND);
    }

    private List<SheetRow> readSheetRaw(Long schoolId, String token, Long classroomId) {
        return ok(this.<List<SheetRow>>exchange("/schools/" + schoolId + "/attendance?classroomId=" + classroomId, HttpMethod.GET,
                null, token, new ParameterizedTypeReference<>() {}));
    }

    @Test
    void subscribedAndNonSubscribedStudentsAreBothTrackedButOnlySubscribedGetArrivals() {
        Env env = buildEnv();
        StudentResponse linked = subscribedStudent(env, "Sub", env.childIds().get(0));
        StudentResponse plain = addStudent(env, "Plain", false, null);
        assertThat(linked.subscription()).isEqualTo(Subscription.SUBSCRIBED);
        assertThat(linked.childId()).isEqualTo(env.childIds().get(0));
        assertThat(plain.subscription()).isEqualTo(Subscription.NON_SUBSCRIBED);

        // A child can back only one student, and a linked student cannot silently re-link elsewhere.
        StudentResponse other = addStudent(env, "Other", false, null);
        assertStatus(this.<StudentResponse>exchange("/schools/" + env.schoolId() + "/students/" + other.id() + "/link-child",
                HttpMethod.POST, new LinkChildRequest(env.childIds().get(0)), env.adminToken(), new ParameterizedTypeReference<>() {}),
                HttpStatus.CONFLICT);
        assertStatus(this.<StudentResponse>exchange("/schools/" + env.schoolId() + "/students/" + linked.id() + "/link-child",
                HttpMethod.POST, new LinkChildRequest(env.childIds().get(1)), env.adminToken(), new ParameterizedTypeReference<>() {}),
                HttpStatus.CONFLICT);

        // Filters.
        PageResponse<StudentResponse> subscribedOnly = ok(this.<PageResponse<StudentResponse>>exchange(
                "/schools/" + env.schoolId() + "/students?subscription=SUBSCRIBED", HttpMethod.GET, null, env.teacherToken(),
                new ParameterizedTypeReference<>() {}));
        assertThat(subscribedOnly.content()).extracting(StudentResponse::id).containsExactly(linked.id());
        PageResponse<StudentResponse> nonSubscribed = ok(this.<PageResponse<StudentResponse>>exchange(
                "/schools/" + env.schoolId() + "/students?subscription=NON_SUBSCRIBED&q=plain", HttpMethod.GET, null,
                env.teacherToken(), new ParameterizedTypeReference<>() {}));
        assertThat(nonSubscribed.content()).extracting(StudentResponse::id).containsExactly(plain.id());

        // Both are marked on the same free sheet.
        LocalDate today = SchoolTime.today();
        ok(putBulk(env.schoolId(), env.teacherToken(), sheet(env, today, linked.id(), AttendanceStatus.PRESENT,
                plain.id(), AttendanceStatus.PRESENT, other.id(), AttendanceStatus.ABSENT)));

        // Bus drops the subscribed child; the non-subscribed students never have an arrival.
        driverRecords(env, env.childIds().get(0), ChildTripEventType.DROPPED_AT_SCHOOL);
        ArrivalResponse arrival = awaitArrival(env, env.childIds().get(0), ArrivalKind.SCHOOL_DROP);
        assertThat(arrival.studentId()).isEqualTo(linked.id());
        // Attendance was taken BEFORE the bus event: the school confirmation is picked up on open.
        assertThat(arrival.schoolConfirm()).isEqualTo(SchoolConfirm.PRESENT);
        assertThat(arrival.outcome()).isEqualTo(ArrivalOutcome.SAFE);

        List<SheetRow> rows = readSheet(env, today);
        SheetRow linkedRow = rows.stream().filter(r -> r.studentId().equals(linked.id())).findFirst().orElseThrow();
        SheetRow plainRow = rows.stream().filter(r -> r.studentId().equals(plain.id())).findFirst().orElseThrow();
        assertThat(linkedRow.subscription()).isEqualTo(Subscription.SUBSCRIBED);
        assertThat(linkedRow.arrival()).isEqualTo(ArrivalOutcome.SAFE);
        assertThat(plainRow.subscription()).isEqualTo(Subscription.NON_SUBSCRIBED);
        assertThat(plainRow.arrival()).isNull();

        AttendanceSummary summary = ok(this.<AttendanceSummary>exchange(
                "/schools/" + env.schoolId() + "/attendance/summary?date=" + today, HttpMethod.GET, null, env.adminToken(),
                new ParameterizedTypeReference<>() {}));
        assertThat(summary.subscribed()).isEqualTo(1);
        assertThat(summary.nonSubscribed()).isEqualTo(2);
        assertThat(summary.mismatches()).isZero();

        // Unlink returns the student to NON_SUBSCRIBED.
        StudentResponse unlinked = ok(this.<StudentResponse>exchange(
                "/schools/" + env.schoolId() + "/students/" + linked.id() + "/link-child", HttpMethod.DELETE, null,
                env.adminToken(), new ParameterizedTypeReference<>() {}));
        assertThat(unlinked.subscription()).isEqualTo(Subscription.NON_SUBSCRIBED);
        assertThat(unlinked.childId()).isNull();
    }

    @Test
    void arrivalOutcomeTransitionsFromDriverEventsSchoolAttendanceAndParentResponses() {
        Env env = buildEnv();
        Long childA = env.childIds().get(0);
        Long childB = env.childIds().get(1);
        Long childC = env.childIds().get(2);
        StudentResponse sA = subscribedStudent(env, "ArrA", childA);
        StudentResponse sB = subscribedStudent(env, "ArrB", childB);
        StudentResponse sC = subscribedStudent(env, "ArrC", childC);
        LocalDate today = SchoolTime.today();

        // BOARDED on a morning trip = HOME_PICKUP, still pending until someone confirms.
        driverRecords(env, childA, ChildTripEventType.BOARDED);
        ArrivalResponse home = awaitArrival(env, childA, ArrivalKind.HOME_PICKUP);
        assertThat(home.outcome()).isEqualTo(ArrivalOutcome.PENDING);
        assertThat(home.schoolConfirm()).isEqualTo(SchoolConfirm.PENDING);
        assertThat(home.parentResponse()).isEqualTo(ParentResponse.PENDING);

        // DROPPED_AT_SCHOOL: parent approval alone is NOT enough for a school drop...
        driverRecords(env, childA, ChildTripEventType.DROPPED_AT_SCHOOL);
        ArrivalResponse drop = awaitArrival(env, childA, ArrivalKind.SCHOOL_DROP);
        assertThat(drop.outcome()).isEqualTo(ArrivalOutcome.PENDING);

        List<ArrivalResponse> pending = ok(this.<List<ArrivalResponse>>exchange("/parents/me/arrivals/pending", HttpMethod.GET,
                null, env.parentToken(), new ParameterizedTypeReference<>() {}));
        assertThat(pending).extracting(ArrivalResponse::id).contains(drop.id(), home.id());

        ArrivalResponse approved = ok(this.<ArrivalResponse>exchange("/parents/me/arrivals/" + drop.id() + "/respond",
                HttpMethod.POST, new RespondRequest(ParentResponse.APPROVED, null), env.parentToken(),
                new ParameterizedTypeReference<>() {}));
        assertThat(approved.parentResponse()).isEqualTo(ParentResponse.APPROVED);
        assertThat(approved.outcome()).isEqualTo(ArrivalOutcome.PENDING);

        // ...the school marking the child PRESENT completes it: SAFE.
        ok(putBulk(env.schoolId(), env.teacherToken(), sheet(env, today, sA.id(), AttendanceStatus.LATE)));
        ArrivalResponse safe = findArrival(env, childA, ArrivalKind.SCHOOL_DROP);
        assertThat(safe.schoolConfirm()).isEqualTo(SchoolConfirm.PRESENT);
        assertThat(safe.outcome()).isEqualTo(ArrivalOutcome.SAFE);
        // Idempotent approve.
        ok(this.<ArrivalResponse>exchange("/parents/me/arrivals/" + drop.id() + "/respond", HttpMethod.POST,
                new RespondRequest(ParentResponse.APPROVED, null), env.parentToken(), new ParameterizedTypeReference<>() {}));

        // MISMATCH: bus says dropped, school says ABSENT -> ALERT, counted in the summary and the ALERT filter.
        driverRecords(env, childB, ChildTripEventType.DROPPED_AT_SCHOOL);
        ArrivalResponse dropB = awaitArrival(env, childB, ArrivalKind.SCHOOL_DROP);
        assertThat(dropB.outcome()).isEqualTo(ArrivalOutcome.PENDING);
        ok(putBulk(env.schoolId(), env.teacherToken(), sheet(env, today, sB.id(), AttendanceStatus.ABSENT)));
        ArrivalResponse alert = findArrival(env, childB, ArrivalKind.SCHOOL_DROP);
        assertThat(alert.schoolConfirm()).isEqualTo(SchoolConfirm.ABSENT);
        assertThat(alert.outcome()).isEqualTo(ArrivalOutcome.ALERT);

        AttendanceSummary summary = ok(this.<AttendanceSummary>exchange(
                "/schools/" + env.schoolId() + "/attendance/summary?date=" + today, HttpMethod.GET, null, env.adminToken(),
                new ParameterizedTypeReference<>() {}));
        assertThat(summary.mismatches()).isEqualTo(1);

        PageResponse<ArrivalResponse> alerts = ok(this.<PageResponse<ArrivalResponse>>exchange(
                "/schools/" + env.schoolId() + "/arrivals?date=" + today + "&outcome=ALERT&size=1000", HttpMethod.GET, null,
                env.adminToken(), new ParameterizedTypeReference<>() {}));
        assertThat(alerts.size()).isEqualTo(100);
        assertThat(alerts.content()).extracting(ArrivalResponse::id).containsExactly(alert.id());
        assertThat(readSheet(env, today)).filteredOn(r -> r.studentId().equals(sB.id())).singleElement()
                .extracting(SheetRow::arrival).isEqualTo(ArrivalOutcome.ALERT);

        // Teacher corrects the mark (child was actually there): ALERT heals back to SAFE.
        ok(putBulk(env.schoolId(), env.teacherToken(), sheet(env, today, sB.id(), AttendanceStatus.PRESENT)));
        assertThat(findArrival(env, childB, ArrivalKind.SCHOOL_DROP).outcome()).isEqualTo(ArrivalOutcome.SAFE);

        // Attendance first, bus event later: ABSENT already on the sheet -> the arrival opens as ALERT.
        ok(putBulk(env.schoolId(), env.teacherToken(), sheet(env, today, sC.id(), AttendanceStatus.ABSENT)));
        driverRecords(env, childC, ChildTripEventType.DROPPED_AT_SCHOOL);
        ArrivalResponse openedAsAlert = awaitArrival(env, childC, ArrivalKind.SCHOOL_DROP);
        assertThat(openedAsAlert.outcome()).isEqualTo(ArrivalOutcome.ALERT);

        // A guardian disputing flags it; a later approval cannot erase the dispute (409); a stranger cannot respond (403).
        ArrivalResponse disputed = ok(this.<ArrivalResponse>exchange("/parents/me/arrivals/" + safe.id() + "/respond",
                HttpMethod.POST, new RespondRequest(ParentResponse.DISPUTED, "My child never got off the bus"), env.parentToken(),
                new ParameterizedTypeReference<>() {}));
        assertThat(disputed.outcome()).isEqualTo(ArrivalOutcome.ALERT);
        assertThat(disputed.parentNote()).isEqualTo("My child never got off the bus");
        assertStatus(this.<ArrivalResponse>exchange("/parents/me/arrivals/" + safe.id() + "/respond", HttpMethod.POST,
                new RespondRequest(ParentResponse.APPROVED, null), env.parentToken(), new ParameterizedTypeReference<>() {}),
                HttpStatus.CONFLICT);
        String strangerToken = newParent(env.schoolId(), env.adminToken());
        assertStatus(this.<ArrivalResponse>exchange("/parents/me/arrivals/" + safe.id() + "/respond", HttpMethod.POST,
                new RespondRequest(ParentResponse.DISPUTED, null), strangerToken, new ParameterizedTypeReference<>() {}),
                HttpStatus.FORBIDDEN);
        assertThat(ok(this.<List<ArrivalResponse>>exchange("/parents/me/arrivals/pending", HttpMethod.GET, null, strangerToken,
                new ParameterizedTypeReference<>() {}))).isEmpty();
        // PENDING is not a valid parent response.
        assertStatus(this.<ArrivalResponse>exchange("/parents/me/arrivals/" + dropB.id() + "/respond", HttpMethod.POST,
                new RespondRequest(ParentResponse.PENDING, null), env.parentToken(), new ParameterizedTypeReference<>() {}),
                HttpStatus.BAD_REQUEST);
    }

    private ArrivalResponse findArrival(Env env, Long childId, ArrivalKind kind) {
        return ok(this.<List<ArrivalResponse>>exchange("/trips/" + env.tripId() + "/arrivals", HttpMethod.GET, null,
                env.driverToken(), new ParameterizedTypeReference<>() {})).stream()
                .filter(a -> a.childId().equals(childId) && a.kind() == kind).findFirst().orElseThrow();
    }

    @Test
    void leadsListOnlyConsentedNonSubscribedGuardiansAndOnlyForPlatformAdmin() {
        Env env = buildEnv();
        String marker = unique("LEAD");
        StudentResponse consented = addStudent(env, "Consented" + marker, true, marker);
        StudentResponse notConsented = addStudent(env, "Declined" + marker, false, marker);
        StudentResponse subscribedConsented = addStudent(env, "Customer" + marker, true, marker);
        ok(this.<StudentResponse>exchange("/schools/" + env.schoolId() + "/students/" + subscribedConsented.id() + "/link-child",
                HttpMethod.POST, new LinkChildRequest(env.childIds().get(0)), env.adminToken(), new ParameterizedTypeReference<>() {}));
        assertThat(consented.marketingConsent()).isTrue();
        assertThat(notConsented.marketingConsent()).isFalse();

        String platformToken = platformAdminToken();
        List<LeadResponse> mine = new ArrayList<>();
        for (int page = 0; page < 200; page++) {
            PageResponse<LeadResponse> result = ok(this.<PageResponse<LeadResponse>>exchange(
                    "/platform-admin/leads?page=" + page + "&size=100", HttpMethod.GET, null, platformToken,
                    new ParameterizedTypeReference<>() {}));
            assertThat(result.size()).isLessThanOrEqualTo(100);
            result.content().stream().filter(l -> marker.equals(l.leadSource())).forEach(mine::add);
            if (result.last()) {
                break;
            }
        }
        assertThat(mine).hasSize(1);
        assertThat(mine.get(0).studentId()).isEqualTo(consented.id());
        assertThat(mine.get(0).name()).isEqualTo("Guardian Consented" + marker);
        assertThat(mine.get(0).phone()).isEqualTo(PHONE);
        assertThat(mine.get(0).school()).isNotBlank();

        // Not for school admins, teachers or parents.
        assertStatus(this.<PageResponse<LeadResponse>>exchange("/platform-admin/leads", HttpMethod.GET, null, env.adminToken(),
                new ParameterizedTypeReference<>() {}), HttpStatus.FORBIDDEN);
        assertStatus(this.<PageResponse<LeadResponse>>exchange("/platform-admin/leads", HttpMethod.GET, null, env.teacherToken(),
                new ParameterizedTypeReference<>() {}), HttpStatus.FORBIDDEN);
        assertStatus(this.<PageResponse<LeadResponse>>exchange("/platform-admin/leads", HttpMethod.GET, null, env.parentToken(),
                new ParameterizedTypeReference<>() {}), HttpStatus.FORBIDDEN);
    }
}

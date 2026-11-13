package com.projectx.app;

import static org.assertj.core.api.Assertions.assertThat;

import com.projectx.attendance.dto.AttendanceDtos.BulkAttendanceRequest;
import com.projectx.attendance.dto.ClassroomDtos.ClassroomRequest;
import com.projectx.attendance.dto.ClassroomDtos.ClassroomResponse;
import com.projectx.attendance.dto.ClassroomDtos.ClassroomSummary;
import com.projectx.attendance.dto.StaffDtos.InviteTeacherRequest;
import com.projectx.attendance.dto.StaffDtos.InviteTeacherResponse;
import com.projectx.attendance.dto.StaffDtos.TeacherResponse;
import com.projectx.attendance.dto.StudentDtos.StudentResponse;
import com.projectx.attendance.dto.TimetableDtos.DaySchedule;
import com.projectx.attendance.dto.TimetableDtos.PeriodRequest;
import com.projectx.attendance.dto.TimetableDtos.PeriodResponse;
import com.projectx.attendance.dto.TimetableDtos.TeacherClassroom;
import com.projectx.attendance.domain.AttendanceStatus;
import com.projectx.attendance.service.SchoolTime;
import com.projectx.common.web.ApiResponse;
import com.projectx.common.web.PageResponse;
import com.projectx.grievance.domain.GrievanceStatus;
import com.projectx.grievance.domain.GrievanceType;
import com.projectx.grievance.dto.CreateGrievanceRequest;
import com.projectx.grievance.dto.GrievanceResponse;
import com.projectx.grievance.dto.UpdateGrievanceStatusRequest;
import com.projectx.identity.dto.AcceptInvitationRequest;
import com.projectx.identity.dto.TokenResponse;
import com.projectx.payment.domain.PayrollRecord;
import com.projectx.payment.dto.BankDetailResponse;
import com.projectx.payment.dto.PayrollRecordResponse;
import com.projectx.payment.dto.UpsertBankDetailsRequest;
import com.projectx.payment.repo.PayrollRecordRepository;
import com.projectx.people.domain.DriverEmploymentStatus;
import com.projectx.people.dto.ChildResponse;
import com.projectx.people.dto.CreateDriverRequest;
import com.projectx.people.dto.DriverResponse;
import com.projectx.people.dto.UpdateDriverEmploymentStatusRequest;
import com.projectx.school.domain.OwnershipType;
import com.projectx.school.domain.SchoolStatus;
import com.projectx.school.domain.VehicleType;
import com.projectx.school.dto.CreateSchoolRequest;
import com.projectx.school.dto.CreateVehicleRequest;
import com.projectx.school.dto.RouteResponse;
import com.projectx.school.dto.RouteStopRequest;
import com.projectx.school.dto.SchoolResponse;
import com.projectx.school.dto.SchoolStatusCounts;
import com.projectx.school.dto.UpdateRouteRequest;
import com.projectx.school.dto.UpdateSchoolStatusRequest;
import com.projectx.school.dto.VehicleResponse;
import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Phase 2 admin UX, backend rules: school status filter + suspension reason, platform-only payments, driver/vehicle/route
 * management, classroom summary counts, teacher management (invite status, resend, one-class-teacher rule), timetable
 * periods, driver bank-detail delete and salary history, grievance types and enrichment.
 */
class Phase2AdminFlowTest extends SchoolAttendanceTestKit {

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    PayrollRecordRepository payrollRecordRepository;

    private String base(Env env) {
        return "/schools/" + env.schoolId();
    }

    private <T> ResponseEntity<ApiResponse<T>> get(String path, String token, ParameterizedTypeReference<ApiResponse<T>> type) {
        return exchange(path, HttpMethod.GET, null, token, type);
    }

    // ------------------------------------------------------------------ A1: school status filter + suspension reason

    /**
     * The integration suite shares one long-lived database across every run ever executed against it (see
     * IntegrationTestBase), so by now it holds far more than 100 ACTIVE schools - a plain "page 0, size 100" listing is not
     * guaranteed to contain a school created just now. Every assertion below that needs to find "my" school goes through
     * {@code q=} on a name unique to this test run instead of scanning a page.
     */
    private Long createNamedSchool(String platformToken, String name, String adminEmail) {
        var request = new CreateSchoolRequest(name, "1 Test Rd", "Pune", null, null, null, null, null, null, null, null, null,
                "Admin", "User", adminEmail, "password123");
        return ok(this.<SchoolResponse>exchange("/schools", HttpMethod.POST, request, platformToken, new ParameterizedTypeReference<>() {})).id();
    }

    private SchoolResponse findByQ(String platformToken, String q) {
        PageResponse<SchoolResponse> page = ok(this.<PageResponse<SchoolResponse>>exchange(
                "/platform-admin/schools?q=" + q + "&size=100", HttpMethod.GET, null, platformToken, new ParameterizedTypeReference<>() {}));
        return page.content().stream().filter(s -> s.name().contains(q)).findFirst()
                .orElseThrow(() -> new AssertionError("school matching q=" + q + " not found in " + page.content()));
    }

    @Test
    void suspendingASchoolRequiresAReasonAndTheStatusFilterShowsOnlyThatStatus() {
        String platform = platformAdminToken();
        String tag = unique("P2Tag");
        Long active = createNamedSchool(platform, tag + " Active", unique("a") + "@test.example");
        Long suspended = createNamedSchool(platform, tag + " Suspended", unique("s") + "@test.example");

        // no reason (or blank) -> 400
        assertStatus(this.<SchoolResponse>exchange("/schools/" + suspended + "/status", HttpMethod.PATCH,
                new UpdateSchoolStatusRequest(SchoolStatus.SUSPENDED, null), platform, new ParameterizedTypeReference<>() {}),
                HttpStatus.BAD_REQUEST);
        assertStatus(this.<SchoolResponse>exchange("/schools/" + suspended + "/status", HttpMethod.PATCH,
                new UpdateSchoolStatusRequest(SchoolStatus.SUSPENDED, "   "), platform, new ParameterizedTypeReference<>() {}),
                HttpStatus.BAD_REQUEST);

        SchoolResponse result = ok(this.<SchoolResponse>exchange("/schools/" + suspended + "/status", HttpMethod.PATCH,
                new UpdateSchoolStatusRequest(SchoolStatus.SUSPENDED, "Unpaid platform fees"), platform, new ParameterizedTypeReference<>() {}));
        assertThat(result.status()).isEqualTo(SchoolStatus.SUSPENDED);
        assertThat(result.statusReason()).isEqualTo("Unpaid platform fees");
        assertThat(result.statusChangedAt()).isNotNull();

        // q=tag scopes this to the 2 schools this test itself just created - see the class-level
        // comment on why an unscoped page scan isn't safe (this exact assertion previously scanned
        // an unfiltered page and failed once enough sibling tests in the same suite run had created
        // enough alphabetically-earlier schools to fill page 0 before this test's own school appeared).
        PageResponse<SchoolResponse> onlySuspended = ok(this.<PageResponse<SchoolResponse>>exchange(
                "/platform-admin/schools?status=SUSPENDED&q=" + tag + "&size=100", HttpMethod.GET, null, platform, new ParameterizedTypeReference<>() {}));
        assertThat(onlySuspended.content()).isNotEmpty().allMatch(s -> s.status() == SchoolStatus.SUSPENDED);
        assertThat(onlySuspended.content()).filteredOn(s -> s.id().equals(suspended)).singleElement()
                .extracting(SchoolResponse::statusReason).isEqualTo("Unpaid platform fees");
        assertThat(onlySuspended.content()).noneMatch(s -> s.id().equals(active));

        PageResponse<SchoolResponse> onlyActive = ok(this.<PageResponse<SchoolResponse>>exchange(
                "/platform-admin/schools?status=ACTIVE&q=" + tag + "&size=100", HttpMethod.GET, null, platform, new ParameterizedTypeReference<>() {}));
        assertThat(onlyActive.content()).allMatch(s -> s.status() == SchoolStatus.ACTIVE);
        assertThat(onlyActive.content()).anyMatch(s -> s.id().equals(active)).noneMatch(s -> s.id().equals(suspended));

        // The size is capped (and paging works) even when a huge size is asked for.
        PageResponse<SchoolResponse> capped = ok(this.<PageResponse<SchoolResponse>>exchange(
                "/platform-admin/schools?size=100000&page=0", HttpMethod.GET, null, platform, new ParameterizedTypeReference<>() {}));
        assertThat(capped.size()).isLessThanOrEqualTo(100);

        SchoolStatusCounts counts = ok(this.<SchoolStatusCounts>exchange("/platform-admin/schools/status-counts", HttpMethod.GET, null,
                platform, new ParameterizedTypeReference<>() {}));
        assertThat(counts.suspended()).isGreaterThanOrEqualTo(1);
        assertThat(counts.all()).isEqualTo(counts.active() + counts.suspended());

        // Re-activating clears the reason.
        SchoolResponse back = ok(this.<SchoolResponse>exchange("/schools/" + suspended + "/status", HttpMethod.PATCH,
                new UpdateSchoolStatusRequest(SchoolStatus.ACTIVE, null), platform, new ParameterizedTypeReference<>() {}));
        assertThat(back.status()).isEqualTo(SchoolStatus.ACTIVE);
        assertThat(back.statusReason()).isNull();

        // Only the platform admin can use these endpoints.
        String schoolAdmin = login(jdbc.queryForObject("SELECT email FROM users WHERE school_id = ? AND role = 'SCHOOL_ADMIN' LIMIT 1",
                String.class, active), "password123");
        assertStatus(this.<Object>exchange("/platform-admin/schools", HttpMethod.GET, null, schoolAdmin, new ParameterizedTypeReference<>() {}),
                HttpStatus.FORBIDDEN);
        assertStatus(this.<SchoolResponse>exchange("/schools/" + active + "/status", HttpMethod.PATCH,
                new UpdateSchoolStatusRequest(SchoolStatus.SUSPENDED, "self"), schoolAdmin, new ParameterizedTypeReference<>() {}),
                HttpStatus.FORBIDDEN);
    }

    // ------------------------------------------------------------------ B1: payments are platform-only

    @Test
    void everyPaymentFeeAndPayrollAdminEndpointIsForbiddenToSchoolAdminButOpenToPlatformAdmin() {
        Env env = buildEnv();
        String platform = platformAdminToken();
        String s = base(env);
        record Call(String name, HttpMethod method, String path, Object body) {}
        List<Call> calls = List.of(
                new Call("get fee schedule", HttpMethod.GET, s + "/fee-schedule", null),
                new Call("put fee schedule", HttpMethod.PUT, s + "/fee-schedule", Map.of("monthlyAmount", 500)),
                new Call("fee invoices", HttpMethod.GET, s + "/fee-invoices", null),
                new Call("school payments", HttpMethod.GET, s + "/payments", null),
                new Call("school payroll", HttpMethod.GET, s + "/payroll", null),
                new Call("set incentive", HttpMethod.PATCH, s + "/payroll/drivers/1/incentive", Map.of("incentiveAmount", 10)),
                new Call("generate invoices", HttpMethod.POST, "/billing/generate-invoices", null),
                new Call("accrue late fees", HttpMethod.POST, "/billing/accrue-late-fees", null),
                new Call("run payroll", HttpMethod.POST, "/billing/run-payroll", null),
                new Call("waiver list", HttpMethod.GET, "/late-fee-waivers", null));
        for (Call c : calls) {
            assertStatus(this.<Object>exchange(c.path(), c.method(), c.body(), env.adminToken(), new ParameterizedTypeReference<>() {}),
                    HttpStatus.FORBIDDEN);
            assertStatus(this.<Object>exchange(c.path(), c.method(), c.body(), env.teacherToken(), new ParameterizedTypeReference<>() {}),
                    HttpStatus.FORBIDDEN);
        }
        // Reads stay open to the platform admin (PUT/POST side effects are not exercised here).
        for (String path : List.of(s + "/fee-invoices", s + "/payments", s + "/payroll")) {
            assertStatus(this.<Object>exchange(path, HttpMethod.GET, null, platform, new ParameterizedTypeReference<>() {}), HttpStatus.OK);
        }
        // Driver and parent keep their own payment-adjacent endpoints.
        assertStatus(this.<Object>exchange("/drivers/me/payroll", HttpMethod.GET, null, env.driverToken(), new ParameterizedTypeReference<>() {}),
                HttpStatus.OK);
        assertStatus(this.<Object>exchange("/parents/me/payments", HttpMethod.GET, null, env.parentToken(), new ParameterizedTypeReference<>() {}),
                HttpStatus.OK);
        // The school dashboard no longer carries money for a school admin.
        Map<String, Object> stats = ok(this.<Map<String, Object>>exchange(s + "/dashboard-stats", HttpMethod.GET, null, env.adminToken(),
                new ParameterizedTypeReference<>() {}));
        assertThat(stats.get("revenueThisMonth")).isNull();
        assertThat(((Number) stats.get("payrollPending")).longValue()).isZero();
    }

    // ------------------------------------------------------------------ B2/B3/B4: vehicles, drivers

    @Test
    void vehiclesCarryOwnerDriverRouteAndStudentCountAndDriversNeedAReasonToBeSuspended() {
        Env env = buildEnv();
        String s = base(env);

        // PRIVATE ownership needs an owner name.
        assertStatus(this.<VehicleResponse>exchange(s + "/vehicles", HttpMethod.POST,
                new CreateVehicleRequest(unique("PRV").toUpperCase(), VehicleType.BUS, 40, null, null, "9876500000", null,
                        OwnershipType.PRIVATE, null), env.adminToken(), new ParameterizedTypeReference<>() {}), HttpStatus.BAD_REQUEST);

        DriverResponse driver = ok(this.<DriverResponse>exchange(s + "/drivers", HttpMethod.POST,
                new CreateDriverRequest("Asha", "Rao", unique("drv") + "@test.example", "password123", "9900000007", unique("LIC"),
                        java.time.LocalDate.now().plusYears(2), null, "12 Park Street"), env.adminToken(), new ParameterizedTypeReference<>() {}));
        assertThat(driver.address()).isEqualTo("12 Park Street");
        assertThat(driver.licenseExpiry()).isNotNull();

        VehicleResponse created = ok(this.<VehicleResponse>exchange(s + "/vehicles", HttpMethod.POST,
                new CreateVehicleRequest(unique("PRV").toUpperCase(), VehicleType.BUS, 40, null, "R. Owner", "9876500000", "Owner Street 1",
                        OwnershipType.PRIVATE, driver.id()), env.adminToken(), new ParameterizedTypeReference<>() {}));
        assertThat(created.ownershipType()).isEqualTo(OwnershipType.PRIVATE);
        assertThat(created.ownerName()).isEqualTo("R. Owner");
        assertThat(created.assignedDriverId()).isEqualTo(driver.id());
        assertThat(created.driverName()).isEqualTo("Asha Rao");

        // Another school's driver cannot be allocated (same 404 as a missing one).
        Env other = buildEnv();
        DriverResponse foreign = ok(this.<DriverResponse>exchange(base(other) + "/drivers", HttpMethod.POST,
                new CreateDriverRequest("Foreign", "Driver", unique("fd") + "@test.example", "password123", "9900000008", unique("LIC"), null, null),
                other.adminToken(), new ParameterizedTypeReference<>() {}));
        assertStatus(this.<VehicleResponse>exchange(s + "/vehicles/" + created.id() + "/driver", HttpMethod.PUT,
                Map.of("driverId", foreign.id()), env.adminToken(), new ParameterizedTypeReference<>() {}), HttpStatus.NOT_FOUND);

        // Students carried = active children riding a route assigned to the vehicle.
        RouteResponse route = ok(this.<List<RouteResponse>>exchange(s + "/routes", HttpMethod.GET, null, env.adminToken(),
                new ParameterizedTypeReference<>() {})).get(0);
        for (Long childId : env.childIds()) {
            ok(this.<ChildResponse>exchange(s + "/children/" + childId + "/route-stop", HttpMethod.PUT,
                    Map.of("routeId", route.id(), "routeStopId", route.stops().get(0).id()), env.adminToken(), new ParameterizedTypeReference<>() {}));
        }
        List<VehicleResponse> vehicles = ok(this.<List<VehicleResponse>>exchange(s + "/vehicles", HttpMethod.GET, null, env.adminToken(),
                new ParameterizedTypeReference<>() {}));
        VehicleResponse routed = vehicles.stream().filter(v -> v.id().equals(route.vehicleId())).findFirst().orElseThrow();
        assertThat(routed.studentCount()).isEqualTo(3);
        assertThat(routed.routeName()).isEqualTo(route.name());
        assertThat(routed.driverName()).isNotNull();
        VehicleResponse unrouted = vehicles.stream().filter(v -> v.id().equals(created.id())).findFirst().orElseThrow();
        assertThat(unrouted.studentCount()).isZero();
        assertThat(unrouted.routeName()).isNull();

        // Unassign through the explicit endpoint.
        VehicleResponse released = ok(this.<VehicleResponse>exchange(s + "/vehicles/" + created.id() + "/driver", HttpMethod.PUT,
                Map.of(), env.adminToken(), new ParameterizedTypeReference<>() {}));
        assertThat(released.assignedDriverId()).isNull();

        // Suspend / terminate need a reason; ACTIVE clears it.
        String statusPath = s + "/drivers/" + driver.id() + "/employment-status";
        assertStatus(this.<DriverResponse>exchange(statusPath, HttpMethod.PATCH,
                new UpdateDriverEmploymentStatusRequest(DriverEmploymentStatus.SUSPENDED, null), env.adminToken(),
                new ParameterizedTypeReference<>() {}), HttpStatus.BAD_REQUEST);
        assertStatus(this.<DriverResponse>exchange(statusPath, HttpMethod.PATCH,
                new UpdateDriverEmploymentStatusRequest(DriverEmploymentStatus.TERMINATED, " "), env.adminToken(),
                new ParameterizedTypeReference<>() {}), HttpStatus.BAD_REQUEST);
        DriverResponse suspended = ok(this.<DriverResponse>exchange(statusPath, HttpMethod.PATCH,
                new UpdateDriverEmploymentStatusRequest(DriverEmploymentStatus.SUSPENDED, "Licence under review"), env.adminToken(),
                new ParameterizedTypeReference<>() {}));
        assertThat(suspended.employmentStatus()).isEqualTo(DriverEmploymentStatus.SUSPENDED);
        assertThat(suspended.statusReason()).isEqualTo("Licence under review");
        assertThat(suspended.statusChangedAt()).isNotNull();
        DriverResponse active = ok(this.<DriverResponse>exchange(statusPath, HttpMethod.PATCH,
                new UpdateDriverEmploymentStatusRequest(DriverEmploymentStatus.ACTIVE, null), env.adminToken(),
                new ParameterizedTypeReference<>() {}));
        assertThat(active.statusReason()).isNull();
        // A suspended driver is not paid by the payroll run (only ACTIVE drivers are) - the status is what payroll keys off.
        DriverResponse terminated = ok(this.<DriverResponse>exchange(statusPath, HttpMethod.PATCH,
                new UpdateDriverEmploymentStatusRequest(DriverEmploymentStatus.TERMINATED, "Contract ended"), env.adminToken(),
                new ParameterizedTypeReference<>() {}));
        assertThat(terminated.employmentStatus()).isEqualTo(DriverEmploymentStatus.TERMINATED);

        // Drivers table: address, license and allocated vehicle number come back in the list.
        List<DriverResponse> drivers = ok(this.<List<DriverResponse>>exchange(s + "/drivers", HttpMethod.GET, null, env.adminToken(),
                new ParameterizedTypeReference<>() {}));
        assertThat(drivers).filteredOn(d -> d.id().equals(driver.id())).singleElement().satisfies(d -> {
            assertThat(d.address()).isEqualTo("12 Park Street");
            assertThat(d.statusReason()).isEqualTo("Contract ended");
        });
        assertThat(drivers).anyMatch(d -> d.vehicleRegistrationNumber() != null);
    }

    // ------------------------------------------------------------------ B5: route edit

    @Test
    void routeEditUpdatesStopsInPlaceAndRefusesToDropAStopChildrenStillUse() {
        Env env = buildEnv();
        String s = base(env);
        RouteResponse route = ok(this.<List<RouteResponse>>exchange(s + "/routes", HttpMethod.GET, null, env.adminToken(),
                new ParameterizedTypeReference<>() {})).get(0);
        assertThat(route.stopCount()).isEqualTo(1);
        assertThat(route.status()).isEqualTo("ACTIVE");
        assertThat(route.vehicleRegistrationNumber()).isNotNull();
        Long firstStop = route.stops().get(0).id();

        RouteResponse edited = ok(this.<RouteResponse>exchange(s + "/routes/" + route.id(), HttpMethod.PUT,
                new UpdateRouteRequest("Renamed route", List.of(
                        new RouteStopRequest(firstStop, 1, "Main Gate (moved)", 18.531, 73.861, 150, LocalTime.of(7, 30)),
                        new RouteStopRequest(null, 2, "Second Stop", 18.54, 73.87, 150, LocalTime.of(7, 45))), 0, 6000, true),
                env.adminToken(), new ParameterizedTypeReference<>() {}));
        assertThat(edited.name()).isEqualTo("Renamed route");
        assertThat(edited.stopCount()).isEqualTo(2);
        assertThat(edited.stops().get(0).id()).isEqualTo(firstStop);
        assertThat(edited.stops().get(0).scheduledTime()).isEqualTo(LocalTime.of(7, 30));
        assertThat(edited.stops().get(1).label()).isEqualTo("Second Stop");

        // A child assigned to the first stop blocks removing it.
        ok(this.<ChildResponse>exchange(s + "/children/" + env.childIds().get(0) + "/route-stop", HttpMethod.PUT,
                Map.of("routeId", route.id(), "routeStopId", firstStop), env.adminToken(), new ParameterizedTypeReference<>() {}));
        Long second = edited.stops().get(1).id();
        assertStatus(this.<RouteResponse>exchange(s + "/routes/" + route.id(), HttpMethod.PUT,
                new UpdateRouteRequest("Renamed route", List.of(
                        new RouteStopRequest(second, 1, "Second Stop", 18.54, 73.87, 150, null)), null, null, null),
                env.adminToken(), new ParameterizedTypeReference<>() {}), HttpStatus.CONFLICT);
        // A foreign stop id is rejected.
        assertStatus(this.<RouteResponse>exchange(s + "/routes/" + route.id(), HttpMethod.PUT,
                new UpdateRouteRequest("Renamed route", List.of(
                        new RouteStopRequest(999999999L, 1, "Ghost", 18.54, 73.87, 150, null)), null, null, null),
                env.adminToken(), new ParameterizedTypeReference<>() {}), HttpStatus.BAD_REQUEST);
        // Another school's admin cannot touch this route.
        Env other = buildEnv();
        assertStatus(this.<RouteResponse>exchange(base(other) + "/routes/" + route.id(), HttpMethod.PUT,
                new UpdateRouteRequest("x", List.of(new RouteStopRequest(1, "x", 18.5, 73.8, 150)), null, null, null),
                other.adminToken(), new ParameterizedTypeReference<>() {}), HttpStatus.NOT_FOUND);
    }

    // ------------------------------------------------------------------ B6: classroom summary

    @Test
    void classroomSummaryCountsStudentsPresentAndAbsentForTheDate() {
        Env env = buildEnv();
        String s = base(env);
        StudentResponse a = addStudent(env, "SumA");
        StudentResponse b = addStudent(env, "SumB");
        StudentResponse c = addStudent(env, "SumC");
        ClassroomResponse empty = ok(this.<ClassroomResponse>exchange(s + "/classrooms", HttpMethod.POST,
                new ClassroomRequest("Empty " + unique(""), "Z"), env.adminToken(), new ParameterizedTypeReference<>() {}));
        ok(bulk(env, env.teacherToken(), SchoolTime.today(), a.id(), AttendanceStatus.PRESENT, b.id(), AttendanceStatus.LATE,
                c.id(), AttendanceStatus.ABSENT));

        List<ClassroomSummary> rows = ok(this.<List<ClassroomSummary>>exchange(s + "/classrooms/summary", HttpMethod.GET, null,
                env.adminToken(), new ParameterizedTypeReference<>() {}));
        ClassroomSummary mine = rows.stream().filter(r -> r.id().equals(env.classroomId())).findFirst().orElseThrow();
        assertThat(mine.totalStudents()).isEqualTo(3);
        assertThat(mine.present()).as("present includes late").isEqualTo(2);
        assertThat(mine.late()).isEqualTo(1);
        assertThat(mine.absent()).isEqualTo(1);
        assertThat(mine.unmarked()).isZero();
        assertThat(mine.classTeacherId()).isEqualTo(jwtSubject(env.teacherToken()));
        assertThat(mine.classTeacherName()).isNotBlank();
        ClassroomSummary none = rows.stream().filter(r -> r.id().equals(empty.id())).findFirst().orElseThrow();
        assertThat(none.totalStudents()).isZero();
        assertThat(none.present()).isZero();
        assertThat(none.classTeacherId()).isNull();

        // A different date has no marks; the date parameter is honoured.
        List<ClassroomSummary> yesterday = ok(this.<List<ClassroomSummary>>exchange(
                s + "/classrooms/summary?date=" + SchoolTime.today().minusDays(1), HttpMethod.GET, null, env.adminToken(),
                new ParameterizedTypeReference<>() {}));
        assertThat(yesterday.stream().filter(r -> r.id().equals(env.classroomId())).findFirst().orElseThrow().unmarked()).isEqualTo(3);

        // A teacher sees only their own class; another school's admin is refused.
        List<ClassroomSummary> forTeacher = ok(this.<List<ClassroomSummary>>exchange(s + "/classrooms/summary", HttpMethod.GET, null,
                env.teacherToken(), new ParameterizedTypeReference<>() {}));
        assertThat(forTeacher).extracting(ClassroomSummary::id).containsExactly(env.classroomId());
        Env other = buildEnv();
        assertStatus(this.<Object>exchange(s + "/classrooms/summary", HttpMethod.GET, null, other.adminToken(), new ParameterizedTypeReference<>() {}),
                HttpStatus.FORBIDDEN);

        // The classroom list carries the teacher name and an aggregate studentCount.
        List<ClassroomResponse> list = ok(this.<List<ClassroomResponse>>exchange(s + "/classrooms", HttpMethod.GET, null, env.adminToken(),
                new ParameterizedTypeReference<>() {}));
        assertThat(list).filteredOn(r -> r.id().equals(env.classroomId())).singleElement().satisfies(r -> {
            assertThat(r.studentCount()).isEqualTo(3);
            assertThat(r.classTeacherName()).isNotBlank();
        });
    }

    // ------------------------------------------------------------------ B8/B9, C1: teachers, class teacher, periods

    @Test
    void teacherInviteStatusResendOneClassTeacherRuleAndPeriods() {
        Env env = buildEnv();
        String s = base(env);
        ClassroomResponse second = ok(this.<ClassroomResponse>exchange(s + "/classrooms", HttpMethod.POST,
                new ClassroomRequest("Second " + unique(""), "B"), env.adminToken(), new ParameterizedTypeReference<>() {}));

        // The kit's teacher is already class teacher of the kit classroom: assigning them a second class is a 409.
        Long kitTeacher = jwtSubject(env.teacherToken());
        assertStatus(this.<ClassroomResponse>exchange(s + "/classrooms/" + second.id() + "/class-teacher", HttpMethod.PUT,
                Map.of("teacherId", kitTeacher), env.adminToken(), new ParameterizedTypeReference<>() {}), HttpStatus.CONFLICT);

        // Invite a profile-carrying teacher who becomes class teacher of the second classroom right away.
        String email = unique("newteacher") + "@test.example";
        InviteTeacherResponse invite = ok(this.<InviteTeacherResponse>exchange(s + "/teachers", HttpMethod.POST,
                new InviteTeacherRequest(email, "9811111111", "Meera Iyer", "B.Ed, M.Sc", second.id()), env.adminToken(),
                new ParameterizedTypeReference<>() {}));
        TeacherResponse row = teacher(env, invite.userId());
        assertThat(row.name()).isEqualTo("Meera Iyer");
        assertThat(row.qualification()).isEqualTo("B.Ed, M.Sc");
        assertThat(row.phone()).isEqualTo("9811111111");
        assertThat(row.inviteStatus()).isEqualTo("INVITED");
        assertThat(row.inviteExpiresAt()).isNotNull();
        assertThat(row.classTeacherOfClassroomId()).isEqualTo(second.id());
        assertThat(row.periodsPerWeek()).isZero();

        // A second invitee cannot also be class teacher of the same class; and the failed invite leaves no half-created teacher.
        String ghostEmail = unique("ghost") + "@test.example";
        assertStatus(this.<InviteTeacherResponse>exchange(s + "/teachers", HttpMethod.POST,
                new InviteTeacherRequest(ghostEmail, null, "Ghost T", null, env.classroomId()), env.adminToken(),
                new ParameterizedTypeReference<>() {}), HttpStatus.CONFLICT);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM users WHERE email = ?", Integer.class, ghostEmail)).isZero();

        // Expire the invitation (as time would): the table now says EXPIRED.
        jdbc.update("UPDATE invitation_tokens SET expires_at = now() - interval '1 hour' WHERE user_id = ?", invite.userId());
        assertThat(teacher(env, invite.userId()).inviteStatus()).isEqualTo("EXPIRED");
        assertStatus(this.<TokenResponse>exchange("/auth/invitations/" + invite.inviteToken() + "/accept", HttpMethod.POST,
                new AcceptInvitationRequest("password123"), null, new ParameterizedTypeReference<>() {}), HttpStatus.UNAUTHORIZED);

        // Resend: fresh link works, status flips back to INVITED, the old link stays dead, then accepting makes them ACTIVE.
        InviteTeacherResponse resent = ok(this.<InviteTeacherResponse>exchange(s + "/teachers/" + invite.userId() + "/resend-invite",
                HttpMethod.POST, null, env.adminToken(), new ParameterizedTypeReference<>() {}));
        assertThat(resent.inviteToken()).isNotEqualTo(invite.inviteToken());
        assertThat(teacher(env, invite.userId()).inviteStatus()).isEqualTo("INVITED");
        InviteTeacherResponse resentAgain = ok(this.<InviteTeacherResponse>exchange(s + "/teachers/" + invite.userId() + "/resend-invite",
                HttpMethod.POST, null, env.adminToken(), new ParameterizedTypeReference<>() {}));
        assertStatus(this.<TokenResponse>exchange("/auth/invitations/" + resent.inviteToken() + "/accept", HttpMethod.POST,
                new AcceptInvitationRequest("password123"), null, new ParameterizedTypeReference<>() {}), HttpStatus.UNAUTHORIZED);
        String newTeacherToken = ok(this.<TokenResponse>exchange("/auth/invitations/" + resentAgain.inviteToken() + "/accept", HttpMethod.POST,
                new AcceptInvitationRequest("password123"), null, new ParameterizedTypeReference<>() {})).accessToken();
        assertThat(teacher(env, invite.userId()).inviteStatus()).isEqualTo("ACTIVE");
        assertStatus(this.<InviteTeacherResponse>exchange(s + "/teachers/" + invite.userId() + "/resend-invite", HttpMethod.POST, null,
                env.adminToken(), new ParameterizedTypeReference<>() {}), HttpStatus.CONFLICT);

        // Edit: change qualification and give up the class-teacher role.
        TeacherResponse edited = ok(this.<TeacherResponse>exchange(s + "/teachers/" + invite.userId(), HttpMethod.PATCH,
                Map.of("name", "Meera I. Iyer", "phone", "9822222222", "qualification", "PhD"), env.adminToken(),
                new ParameterizedTypeReference<>() {}));
        assertThat(edited.name()).isEqualTo("Meera I. Iyer");
        assertThat(edited.qualification()).isEqualTo("PhD");
        assertThat(edited.classTeacherOfClassroomId()).isNull();

        // Periods: create for the kit classroom, overlaps refused, listing per teacher, then delete.
        PeriodResponse p1 = ok(this.<PeriodResponse>exchange(s + "/classrooms/" + env.classroomId() + "/periods", HttpMethod.POST,
                new PeriodRequest(invite.userId(), "Maths", 1, LocalTime.of(9, 0), LocalTime.of(9, 45)), env.adminToken(),
                new ParameterizedTypeReference<>() {}));
        assertThat(p1.classroomName()).isNotBlank();
        assertThat(p1.teacherName()).isEqualTo("Meera I. Iyer");
        ok(this.<PeriodResponse>exchange(s + "/classrooms/" + env.classroomId() + "/periods", HttpMethod.POST,
                new PeriodRequest(invite.userId(), "Science", 1, LocalTime.of(10, 0), LocalTime.of(10, 45)), env.adminToken(),
                new ParameterizedTypeReference<>() {}));
        ok(this.<PeriodResponse>exchange(s + "/classrooms/" + second.id() + "/periods", HttpMethod.POST,
                new PeriodRequest(invite.userId(), "Maths", 3, LocalTime.of(9, 0), LocalTime.of(9, 45)), env.adminToken(),
                new ParameterizedTypeReference<>() {}));
        assertStatus(this.<PeriodResponse>exchange(s + "/classrooms/" + second.id() + "/periods", HttpMethod.POST,
                new PeriodRequest(invite.userId(), "Overlap", 1, LocalTime.of(9, 30), LocalTime.of(10, 15)), env.adminToken(),
                new ParameterizedTypeReference<>() {}), HttpStatus.CONFLICT);
        assertStatus(this.<PeriodResponse>exchange(s + "/classrooms/" + second.id() + "/periods", HttpMethod.POST,
                new PeriodRequest(invite.userId(), "Backwards", 2, LocalTime.of(10, 0), LocalTime.of(9, 0)), env.adminToken(),
                new ParameterizedTypeReference<>() {}), HttpStatus.BAD_REQUEST);
        assertStatus(this.<PeriodResponse>exchange(s + "/classrooms/" + second.id() + "/periods", HttpMethod.POST,
                new PeriodRequest(invite.userId(), "BadDay", 9, LocalTime.of(9, 0), LocalTime.of(9, 45)), env.adminToken(),
                new ParameterizedTypeReference<>() {}), HttpStatus.BAD_REQUEST);
        assertThat(teacher(env, invite.userId()).periodsPerWeek()).isEqualTo(3);
        List<PeriodResponse> mine = ok(this.<List<PeriodResponse>>exchange(s + "/teachers/" + invite.userId() + "/periods", HttpMethod.GET,
                null, env.adminToken(), new ParameterizedTypeReference<>() {}));
        assertThat(mine).hasSize(3);

        // The teacher's own views: periods grouped by day, classes with role + subjects.
        List<DaySchedule> days = ok(this.<List<DaySchedule>>exchange("/teachers/me/periods", HttpMethod.GET, null, newTeacherToken,
                new ParameterizedTypeReference<>() {}));
        assertThat(days).extracting(DaySchedule::dayOfWeek).containsExactly(1, 3);
        assertThat(days.get(0).periods()).extracting(PeriodResponse::subject).containsExactly("Maths", "Science");
        List<TeacherClassroom> classes = ok(this.<List<TeacherClassroom>>exchange("/teachers/me/classrooms", HttpMethod.GET, null,
                newTeacherToken, new ParameterizedTypeReference<>() {}));
        assertThat(classes).extracting(c -> c.classroom().id()).containsExactlyInAnyOrder(env.classroomId(), second.id());
        assertThat(classes).allMatch(c -> c.role().equals("LECTURER") && !c.subjects().isEmpty());

        // A teacher cannot manage periods or other teachers; a foreign school's admin is refused.
        assertStatus(this.<PeriodResponse>exchange(s + "/classrooms/" + env.classroomId() + "/periods", HttpMethod.POST,
                new PeriodRequest(invite.userId(), "Hack", 4, LocalTime.of(9, 0), LocalTime.of(9, 45)), newTeacherToken,
                new ParameterizedTypeReference<>() {}), HttpStatus.FORBIDDEN);
        Env other = buildEnv();
        assertStatus(this.<Object>exchange(s + "/teachers/" + invite.userId() + "/periods", HttpMethod.GET, null, other.adminToken(),
                new ParameterizedTypeReference<>() {}), HttpStatus.FORBIDDEN);
        assertStatus(this.<Object>exchange(base(other) + "/teachers/" + invite.userId() + "/periods", HttpMethod.GET, null, other.adminToken(),
                new ParameterizedTypeReference<>() {}), HttpStatus.NOT_FOUND);

        ok(this.<Object>exchange(s + "/periods/" + p1.id(), HttpMethod.DELETE, null, env.adminToken(), new ParameterizedTypeReference<>() {}));
        assertThat(teacher(env, invite.userId()).periodsPerWeek()).isEqualTo(2);
        assertStatus(this.<Object>exchange(s + "/periods/" + p1.id(), HttpMethod.DELETE, null, env.adminToken(), new ParameterizedTypeReference<>() {}),
                HttpStatus.NOT_FOUND);
    }

    private TeacherResponse teacher(Env env, Long userId) {
        return ok(this.<List<TeacherResponse>>exchange(base(env) + "/teachers", HttpMethod.GET, null, env.adminToken(),
                new ParameterizedTypeReference<>() {})).stream().filter(t -> t.userId().equals(userId)).findFirst().orElseThrow();
    }

    // ------------------------------------------------------------------ D2/D3: bank details and salary history

    @Test
    void driverCanDeleteTheirBankDetailsAndSalaryHistoryShowsTransactionIds() {
        Env env = buildEnv();
        UpsertBankDetailsRequest bank = new UpsertBankDetailsRequest("Test Driver", String.valueOf(System.nanoTime()).substring(0, 12), "HDFC0001234");
        BankDetailResponse saved = ok(this.<BankDetailResponse>exchange("/drivers/me/bank-details", HttpMethod.PUT, bank, env.driverToken(),
                new ParameterizedTypeReference<>() {}));
        assertThat(saved.maskedAccountNumber()).endsWith(bank.accountNumber().substring(8));
        // Upsert keeps ONE record per driver.
        ok(this.<BankDetailResponse>exchange("/drivers/me/bank-details", HttpMethod.PUT, bank, env.driverToken(), new ParameterizedTypeReference<>() {}));
        assertThat(jdbc.queryForObject("SELECT count(*) FROM driver_bank_details WHERE driver_id = ?", Integer.class, saved.driverId())).isEqualTo(1);

        // Salary history: a paid row exposes transactionId + payoutAmount.
        PayrollRecord paid = new PayrollRecord(saved.driverId(), java.time.LocalDate.now().withDayOfMonth(1).minusMonths(1), new BigDecimal("20000.00"));
        paid.markPaid(new BigDecimal("20500.00"), "pout_TEST123");
        payrollRecordRepository.save(paid);
        PayrollRecord pending = new PayrollRecord(saved.driverId(), java.time.LocalDate.now().withDayOfMonth(1), new BigDecimal("20000.00"));
        payrollRecordRepository.save(pending);
        List<PayrollRecordResponse> history = ok(this.<List<PayrollRecordResponse>>exchange("/drivers/me/payroll", HttpMethod.GET, null,
                env.driverToken(), new ParameterizedTypeReference<>() {}));
        assertThat(history).filteredOn(r -> "pout_TEST123".equals(r.transactionId())).singleElement().satisfies(r -> {
            assertThat(r.payoutAmount()).isEqualByComparingTo("20500.00");
            assertThat(r.disbursedAt()).isNotNull();
            assertThat(r.transactionReference()).isEqualTo("pout_TEST123");
        });
        assertThat(history).filteredOn(r -> r.transactionId() == null).singleElement()
                .extracting(PayrollRecordResponse::payoutAmount).satisfies(a -> assertThat(a).isEqualByComparingTo("20000.00"));

        // A payout in flight blocks the delete.
        PayrollRecord processing = new PayrollRecord(saved.driverId(), java.time.LocalDate.now().withDayOfMonth(1).minusMonths(2), new BigDecimal("1.00"));
        processing.markProcessing();
        payrollRecordRepository.save(processing);
        assertStatus(this.<Object>exchange("/drivers/me/bank-details", HttpMethod.DELETE, null, env.driverToken(), new ParameterizedTypeReference<>() {}),
                HttpStatus.CONFLICT);
        payrollRecordRepository.delete(processing);

        // Only the driver can delete; then it is gone (GET 404 -> UI shows the empty add-form) and a second delete is 404.
        assertStatus(this.<Object>exchange("/drivers/me/bank-details", HttpMethod.DELETE, null, env.adminToken(), new ParameterizedTypeReference<>() {}),
                HttpStatus.FORBIDDEN);
        assertStatus(this.<Object>exchange("/drivers/me/bank-details", HttpMethod.DELETE, null, env.parentToken(), new ParameterizedTypeReference<>() {}),
                HttpStatus.FORBIDDEN);
        ok(this.<Object>exchange("/drivers/me/bank-details", HttpMethod.DELETE, null, env.driverToken(), new ParameterizedTypeReference<>() {}));
        assertStatus(this.<BankDetailResponse>exchange("/drivers/me/bank-details", HttpMethod.GET, null, env.driverToken(),
                new ParameterizedTypeReference<>() {}), HttpStatus.NOT_FOUND);
        assertStatus(this.<Object>exchange("/drivers/me/bank-details", HttpMethod.DELETE, null, env.driverToken(), new ParameterizedTypeReference<>() {}),
                HttpStatus.NOT_FOUND);
        // ... and the add-form works again.
        ok(this.<BankDetailResponse>exchange("/drivers/me/bank-details", HttpMethod.PUT, bank, env.driverToken(), new ParameterizedTypeReference<>() {}));
    }

    // ------------------------------------------------------------------ A3 / D1: grievance types and enrichment

    @Test
    void grievanceTypesAreValidatedPerRoleLinksAreVerifiedAndTheListIsEnriched() {
        Env env = buildEnv();
        String s = base(env);
        VehicleResponse vehicle = ok(this.<List<VehicleResponse>>exchange(s + "/vehicles", HttpMethod.GET, null, env.adminToken(),
                new ParameterizedTypeReference<>() {})).get(0);
        DriverResponse driver = ok(this.<List<DriverResponse>>exchange(s + "/drivers", HttpMethod.GET, null, env.adminToken(),
                new ParameterizedTypeReference<>() {})).get(0);
        Long child = env.childIds().get(0);

        // Driver: DRIVER_BEHAVIOR is a parent's complaint -> 400; student/parent behaviour is allowed.
        assertStatus(raise(env.driverToken(), env, GrievanceType.DRIVER_BEHAVIOR, null, null, null, null), HttpStatus.BAD_REQUEST);
        GrievanceResponse byDriver = ok(raise(env.driverToken(), env, GrievanceType.STUDENT_PARENT_BEHAVIOR, null, vehicle.id(), null, child));
        assertThat(byDriver.type()).isEqualTo(GrievanceType.STUDENT_PARENT_BEHAVIOR);
        assertThat(byDriver.complainantName()).contains("Driver");
        assertThat(byDriver.vehicleNo()).isEqualTo(vehicle.registrationNumber());
        assertThat(byDriver.childName()).startsWith("Kid0");
        assertThat(byDriver.schoolName()).isNotBlank();

        // Parent: STUDENT_PARENT_BEHAVIOR is a driver's complaint -> 400; DRIVER_BEHAVIOR with a link to the driver is fine.
        assertStatus(raise(env.parentToken(), env, GrievanceType.STUDENT_PARENT_BEHAVIOR, null, null, null, null), HttpStatus.BAD_REQUEST);
        GrievanceResponse byParent = ok(raise(env.parentToken(), env, GrievanceType.DRIVER_BEHAVIOR, null, null, driver.id(), child));
        assertThat(byParent.driverName()).isEqualTo(driver.firstName() + " " + driver.lastName());
        assertThat(byParent.raisedByRole().name()).isEqualTo("PARENT");
        assertThat(byParent.daysOpen()).isZero();

        // OTHER needs the free text.
        assertStatus(raise(env.driverToken(), env, GrievanceType.OTHER, null, null, null, null), HttpStatus.BAD_REQUEST);
        assertThat(ok(raise(env.driverToken(), env, GrievanceType.OTHER, "Gate was locked", null, null, null)).otherDescription())
                .isEqualTo("Gate was locked");

        // Links are verified: another school's vehicle / a child that is not yours -> 400, not a leak.
        Env other = buildEnv();
        VehicleResponse foreignVehicle = ok(this.<List<VehicleResponse>>exchange(base(other) + "/vehicles", HttpMethod.GET, null, other.adminToken(),
                new ParameterizedTypeReference<>() {})).get(0);
        assertStatus(raise(env.driverToken(), env, GrievanceType.VEHICLE_CONDITION, null, foreignVehicle.id(), null, null), HttpStatus.BAD_REQUEST);
        assertStatus(raise(env.parentToken(), env, GrievanceType.VEHICLE_CONDITION, null, foreignVehicle.id(), null, null), HttpStatus.BAD_REQUEST);
        assertStatus(raise(other.parentToken(), env, GrievanceType.DRIVER_BEHAVIOR, null, null, null, child), HttpStatus.BAD_REQUEST);

        // List DTO for the school admin, with the status filter and paging; detail carries the timeline.
        GrievanceResponse resolved = ok(this.<GrievanceResponse>exchange("/grievances/" + byParent.id() + "/status", HttpMethod.PATCH,
                new UpdateGrievanceStatusRequest(GrievanceStatus.RESOLVED, "Spoke to the driver"), env.adminToken(), new ParameterizedTypeReference<>() {}));
        assertThat(resolved.timeline()).extracting(GrievanceResponse.TimelineEntry::status).containsExactly(GrievanceStatus.OPEN, GrievanceStatus.RESOLVED);
        PageResponse<GrievanceResponse> openOnly = ok(this.<PageResponse<GrievanceResponse>>exchange(
                s + "/grievances/search?status=OPEN&size=100", HttpMethod.GET, null, env.adminToken(), new ParameterizedTypeReference<>() {}));
        assertThat(openOnly.content()).isNotEmpty().allMatch(g -> g.status() == GrievanceStatus.OPEN)
                .noneMatch(g -> g.id().equals(byParent.id()));
        PageResponse<GrievanceResponse> byType = ok(this.<PageResponse<GrievanceResponse>>exchange(
                s + "/grievances/search?type=OTHER", HttpMethod.GET, null, env.adminToken(), new ParameterizedTypeReference<>() {}));
        assertThat(byType.content()).isNotEmpty().allMatch(g -> g.type() == GrievanceType.OTHER);
        GrievanceResponse detail = ok(this.<GrievanceResponse>exchange("/grievances/" + byParent.id(), HttpMethod.GET, null, env.adminToken(),
                new ParameterizedTypeReference<>() {}));
        assertThat(detail.timeline()).hasSize(2);
        // The raiser may open their own; a foreign school's admin and another parent get a 404.
        assertStatus(this.<GrievanceResponse>exchange("/grievances/" + byParent.id(), HttpMethod.GET, null, env.parentToken(),
                new ParameterizedTypeReference<>() {}), HttpStatus.OK);
        assertStatus(this.<GrievanceResponse>exchange("/grievances/" + byParent.id(), HttpMethod.GET, null, other.adminToken(),
                new ParameterizedTypeReference<>() {}), HttpStatus.NOT_FOUND);
        assertStatus(this.<GrievanceResponse>exchange("/grievances/" + byParent.id(), HttpMethod.GET, null, other.parentToken(),
                new ParameterizedTypeReference<>() {}), HttpStatus.NOT_FOUND);
        // The platform-wide paged search is platform-admin only; a school admin cannot widen it.
        assertStatus(this.<Object>exchange("/platform-admin/grievances", HttpMethod.GET, null, env.adminToken(), new ParameterizedTypeReference<>() {}),
                HttpStatus.FORBIDDEN);
        PageResponse<GrievanceResponse> all = ok(this.<PageResponse<GrievanceResponse>>exchange(
                "/platform-admin/grievances?schoolId=" + env.schoolId() + "&size=100", HttpMethod.GET, null, platformAdminToken(),
                new ParameterizedTypeReference<>() {}));
        assertThat(all.content()).extracting(GrievanceResponse::id).contains(byParent.id(), byDriver.id());

        // Complainant profile summary: platform-wide for the platform admin, school-scoped for a school admin.
        Map<String, Object> parentCard = ok(this.<Map<String, Object>>exchange("/platform-admin/users/" + byParent.raisedByUserId() + "/profile-summary",
                HttpMethod.GET, null, platformAdminToken(), new ParameterizedTypeReference<>() {}));
        assertThat(parentCard.get("role")).isEqualTo("PARENT");
        assertThat((List<?>) parentCard.get("children")).hasSize(3);
        Map<String, Object> driverCard = ok(this.<Map<String, Object>>exchange(s + "/users/" + byDriver.raisedByUserId() + "/profile-summary",
                HttpMethod.GET, null, env.adminToken(), new ParameterizedTypeReference<>() {}));
        assertThat(driverCard.get("role")).isEqualTo("DRIVER");
        assertThat(driverCard.get("driver")).isNotNull();
        assertStatus(this.<Object>exchange(base(other) + "/users/" + byDriver.raisedByUserId() + "/profile-summary", HttpMethod.GET, null,
                other.adminToken(), new ParameterizedTypeReference<>() {}), HttpStatus.NOT_FOUND);
        assertStatus(this.<Object>exchange("/platform-admin/users/" + byDriver.raisedByUserId() + "/profile-summary", HttpMethod.GET, null,
                env.adminToken(), new ParameterizedTypeReference<>() {}), HttpStatus.FORBIDDEN);
    }

    private ResponseEntity<ApiResponse<GrievanceResponse>> raise(String token, Env env, GrievanceType type, String other, Long vehicleId,
                                                                  Long driverId, Long childId) {
        return exchange("/grievances", HttpMethod.POST,
                new CreateGrievanceRequest(env.schoolId(), null, type, other, vehicleId, driverId, childId, null, null, "Details of the problem."),
                token, new ParameterizedTypeReference<>() {});
    }

    // ------------------------------------------------------------------ E4: inquiries

    @Test
    void publicInquiryIsStoredAndOnlyThePlatformAdminCanListIt() {
        String unique = unique("Visitor");
        ResponseEntity<ApiResponse<Map<String, Object>>> response = exchange("/public/inquiries", HttpMethod.POST,
                Map.of("name", unique, "contact", "visitor@example.com", "message", "Please call me about transport for my school."),
                null, new ParameterizedTypeReference<>() {});
        assertStatus(response, HttpStatus.CREATED);
        assertThat(response.getBody().data()).containsEntry("received", true).doesNotContainKey("id");

        // honeypot: acknowledged, never stored
        String bot = unique("Bot");
        assertStatus(this.<Map<String, Object>>exchange("/public/inquiries", HttpMethod.POST,
                Map.of("name", bot, "contact", "bot@example.com", "message", "Buy cheap watches now", "website", "http://spam.example"),
                null, new ParameterizedTypeReference<>() {}), HttpStatus.CREATED);
        // validation
        assertStatus(this.<Map<String, Object>>exchange("/public/inquiries", HttpMethod.POST, Map.of("name", "", "contact", "x", "message", "hi"), null,
                new ParameterizedTypeReference<>() {}), HttpStatus.BAD_REQUEST);
        assertStatus(this.<Map<String, Object>>exchange("/public/inquiries", HttpMethod.POST, Map.of("name", "A", "contact", "a@b.example",
                "message", "x".repeat(2001)), null, new ParameterizedTypeReference<>() {}), HttpStatus.BAD_REQUEST);

        PageResponse<Map<String, Object>> page = ok(this.<PageResponse<Map<String, Object>>>exchange("/platform-admin/inquiries?size=100",
                HttpMethod.GET, null, platformAdminToken(), new ParameterizedTypeReference<>() {}));
        assertThat(page.content()).anyMatch(i -> unique.equals(i.get("name")) && "NEW".equals(i.get("status")));
        assertThat(page.content()).noneMatch(i -> bot.equals(i.get("name")));

        Env env = buildEnv();
        for (String token : List.of(env.adminToken(), env.teacherToken(), env.parentToken(), env.driverToken())) {
            assertStatus(this.<Object>exchange("/platform-admin/inquiries", HttpMethod.GET, null, token, new ParameterizedTypeReference<>() {}),
                    HttpStatus.FORBIDDEN);
        }
        assertStatus(this.<Object>exchange("/platform-admin/inquiries", HttpMethod.GET, null, null, new ParameterizedTypeReference<>() {}),
                HttpStatus.UNAUTHORIZED);

        Number id = (Number) page.content().stream().filter(i -> unique.equals(i.get("name"))).findFirst().orElseThrow().get("id");
        Map<String, Object> handled = ok(this.<Map<String, Object>>exchange("/platform-admin/inquiries/" + id + "/status", HttpMethod.PATCH,
                Map.of("status", "HANDLED"), platformAdminToken(), new ParameterizedTypeReference<>() {}));
        assertThat(handled.get("status")).isEqualTo("HANDLED");
    }
}

package com.projectx.app;

import com.projectx.attendance.domain.ArrivalKind;
import com.projectx.attendance.domain.AttendanceStatus;
import com.projectx.attendance.dto.ArrivalDtos.ArrivalResponse;
import com.projectx.attendance.dto.AttendanceDtos.BulkAttendanceRequest;
import com.projectx.attendance.dto.AttendanceDtos.BulkAttendanceResult;
import com.projectx.attendance.dto.AttendanceDtos.BulkEntry;
import com.projectx.attendance.dto.ClassroomDtos.ClassroomRequest;
import com.projectx.attendance.dto.ClassroomDtos.ClassroomResponse;
import com.projectx.attendance.dto.StaffDtos.InviteTeacherRequest;
import com.projectx.attendance.dto.StaffDtos.InviteTeacherResponse;
import com.projectx.attendance.dto.StudentDtos.LinkChildRequest;
import com.projectx.attendance.dto.StudentDtos.StudentRequest;
import com.projectx.attendance.dto.StudentDtos.StudentResponse;
import com.projectx.attendance.service.SchoolTime;
import com.projectx.common.events.ChildTripEventType;
import com.projectx.common.web.ApiResponse;
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
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;

/** Shared world-building for the school-attendance integration tests: a school with admin, teacher, driver on a started trip, a parent with three children. */
abstract class SchoolAttendanceTestKit extends IntegrationTestBase {

    protected static final String PHONE = "9876543210";

    protected record Env(Long schoolId, String adminToken, String teacherToken, String driverToken, String parentToken,
                         Long parentUserId, Long driverUserId, Long classroomId, Long tripId, List<Long> childIds) {}

    protected <T> T ok(ResponseEntity<ApiResponse<T>> response) {
        assertSuccess(response);
        return response.getBody().data();
    }

    /** The JWT subject (= users.id) without verifying the signature - test-side convenience only. */
    protected static Long subjectOf(String jwt) {
        String payload = new String(Base64.getUrlDecoder().decode(jwt.split("\\.")[1]));
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("\"sub\"\\s*:\\s*\"?(\\d+)").matcher(payload);
        if (!m.find()) {
            throw new AssertionError("no sub in " + payload);
        }
        return Long.valueOf(m.group(1));
    }

    protected Long createSchool(String platformToken, String adminEmail) {
        var request = new CreateSchoolRequest(unique("Kit School "), "1 Test Rd", "Pune", null, null,
                null, null, null, 18.5204, 73.8567, 300, null, "Admin", "User", adminEmail, "password123");
        return ok(this.<SchoolResponse>exchange("/schools", HttpMethod.POST, request, platformToken,
                new ParameterizedTypeReference<>() {})).id();
    }

    protected String newParent(Long schoolId, String adminToken) {
        InviteParentResponse invite = ok(this.<InviteParentResponse>exchange("/schools/" + schoolId + "/parents/invitations",
                HttpMethod.POST, new InviteParentRequest(unique("parent") + "@test.example", null), adminToken,
                new ParameterizedTypeReference<>() {}));
        return ok(this.<TokenResponse>exchange("/auth/invitations/" + invite.inviteToken() + "/accept", HttpMethod.POST,
                new AcceptInvitationRequest("password123"), null, new ParameterizedTypeReference<>() {})).accessToken();
    }

    protected Env buildEnv() {
        String platformToken = platformAdminToken();
        String adminEmail = unique("kitadmin") + "@test.example";
        Long schoolId = createSchool(platformToken, adminEmail);
        String adminToken = login(adminEmail, "password123");

        InviteTeacherResponse invite = ok(this.<InviteTeacherResponse>exchange("/schools/" + schoolId + "/teachers",
                HttpMethod.POST, new InviteTeacherRequest(unique("kitteacher") + "@test.example", null), adminToken,
                new ParameterizedTypeReference<>() {}));
        String teacherToken = ok(this.<TokenResponse>exchange("/auth/invitations/" + invite.inviteToken() + "/accept",
                HttpMethod.POST, new AcceptInvitationRequest("password123"), null, new ParameterizedTypeReference<>() {})).accessToken();

        Long classroomId = ok(this.<ClassroomResponse>exchange("/schools/" + schoolId + "/classrooms", HttpMethod.POST,
                new ClassroomRequest("Kit " + unique(""), "A"), adminToken, new ParameterizedTypeReference<>() {})).id();
        makeClassTeacher(schoolId, classroomId, adminToken, teacherToken);

        Long routeId = ok(this.<RouteResponse>exchange("/schools/" + schoolId + "/routes", HttpMethod.POST,
                new CreateRouteRequest(unique("Route "), List.of(new RouteStopRequest(1, "Main Gate", 18.53, 73.86, 150)), 0, 6000),
                adminToken, new ParameterizedTypeReference<>() {})).id();
        Long vehicleId = ok(this.<VehicleResponse>exchange("/schools/" + schoolId + "/vehicles", HttpMethod.POST,
                new CreateVehicleRequest(unique("REG").toUpperCase(), VehicleType.VAN, 12, null), adminToken,
                new ParameterizedTypeReference<>() {})).id();
        String driverEmail = unique("kitdriver") + "@test.example";
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
        return new Env(schoolId, adminToken, teacherToken, driverToken, parentToken, subjectOf(parentToken),
                subjectOf(driverToken), classroomId, tripId, childIds);
    }

    protected StudentResponse addStudent(Env env, String first) {
        var request = new StudentRequest(env.classroomId(), null, first, "Student", null, null, "Guardian " + first, PHONE, null,
                false, null, null);
        return ok(this.<StudentResponse>exchange("/schools/" + env.schoolId() + "/students", HttpMethod.POST, request,
                env.adminToken(), new ParameterizedTypeReference<>() {}));
    }

    protected StudentResponse subscribed(Env env, String first, Long childId) {
        StudentResponse s = addStudent(env, first);
        return linkChild(env, s.id(), childId);
    }

    protected StudentResponse linkChild(Env env, Long studentId, Long childId) {
        return ok(this.<StudentResponse>exchange("/schools/" + env.schoolId() + "/students/" + studentId + "/link-child",
                HttpMethod.POST, new LinkChildRequest(childId), env.adminToken(), new ParameterizedTypeReference<>() {}));
    }

    protected ResponseEntity<ApiResponse<ChildTripEventResponse>> driverEvent(Env env, Long childId, ChildTripEventType type) {
        return exchange("/trips/" + env.tripId() + "/child-events", HttpMethod.POST,
                new RecordChildTripEventRequest(childId, type), env.driverToken(), new ParameterizedTypeReference<>() {});
    }

    protected List<ArrivalResponse> tripArrivals(Env env, String token) {
        return ok(this.<List<ArrivalResponse>>exchange("/trips/" + env.tripId() + "/arrivals", HttpMethod.GET, null,
                token, new ParameterizedTypeReference<>() {}));
    }

    protected ArrivalResponse awaitArrival(Env env, Long childId, ArrivalKind kind) {
        for (int i = 0; i < 50; i++) {
            var match = tripArrivals(env, env.adminToken()).stream()
                    .filter(a -> a.childId().equals(childId) && a.kind() == kind).findFirst();
            if (match.isPresent()) {
                return match.get();
            }
            sleep(200);
        }
        throw new AssertionError("Arrival never appeared for child " + childId + " " + kind);
    }

    protected static void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    protected ResponseEntity<ApiResponse<BulkAttendanceResult>> bulk(Env env, String token, LocalDate date, Object... idAndStatus) {
        List<BulkEntry> entries = new ArrayList<>();
        for (int i = 0; i < idAndStatus.length; i += 2) {
            entries.add(new BulkEntry((Long) idAndStatus[i], (AttendanceStatus) idAndStatus[i + 1]));
        }
        return exchange("/schools/" + env.schoolId() + "/attendance/bulk", HttpMethod.PUT,
                new BulkAttendanceRequest(date, env.classroomId(), entries), token, new ParameterizedTypeReference<>() {});
    }
}

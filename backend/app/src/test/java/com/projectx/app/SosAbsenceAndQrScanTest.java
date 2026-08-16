package com.projectx.app;

import static org.assertj.core.api.Assertions.assertThat;

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
import com.projectx.tracking.dto.CreateTripRequest;
import com.projectx.tracking.dto.SosEventResponse;
import com.projectx.tracking.dto.TripManifestEntry;
import com.projectx.tracking.dto.TripResponse;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

/**
 * Covers the new consumer-trust features end to end: SOS trigger/resolve, the pre-trip absence
 * toggle excluding a child from the driver's manifest, and QR-code boarding (including that a
 * tampered token is rejected, not silently accepted). Same full-stack-HTTP style as
 * GrievanceFlowTest/ChildAutoAssignRouteTest, not mocked.
 */
class SosAbsenceAndQrScanTest extends IntegrationTestBase {

    private record Scenario(Long schoolId, String schoolAdminToken, String driverToken, String parentToken,
                             Long childId, Long tripId) {}

    private Scenario buildScenario() {
        String platformToken = platformAdminToken();

        String schoolAdminEmail = unique("schooladmin") + "@test.example";
        var createSchoolRequest = new CreateSchoolRequest(unique("Sos School "), "1 Test Rd", "Pune", null, null,
                null, null, 18.5204, 73.8567, 300, null, "Admin", "User", schoolAdminEmail, "password123");
        ResponseEntity<ApiResponse<SchoolResponse>> schoolResponse = exchange(
                "/schools", HttpMethod.POST, createSchoolRequest, platformToken, new ParameterizedTypeReference<>() {});
        assertSuccess(schoolResponse);
        Long schoolId = schoolResponse.getBody().data().id();
        String schoolAdminToken = login(schoolAdminEmail, "password123");

        var routeRequest = new CreateRouteRequest(unique("Route "),
                List.of(new RouteStopRequest(1, "Main Gate", 18.53, 73.86, 150)), 0, 6000);
        ResponseEntity<ApiResponse<RouteResponse>> routeResponse = exchange(
                "/schools/" + schoolId + "/routes", HttpMethod.POST, routeRequest, schoolAdminToken,
                new ParameterizedTypeReference<>() {});
        assertSuccess(routeResponse);
        Long routeId = routeResponse.getBody().data().id();
        Long stopId = routeResponse.getBody().data().stops().get(0).id();

        var vehicleRequest = new CreateVehicleRequest(unique("REG").toUpperCase(), VehicleType.VAN, 12, null);
        ResponseEntity<ApiResponse<VehicleResponse>> vehicleResponse = exchange(
                "/schools/" + schoolId + "/vehicles", HttpMethod.POST, vehicleRequest, schoolAdminToken,
                new ParameterizedTypeReference<>() {});
        assertSuccess(vehicleResponse);
        Long vehicleId = vehicleResponse.getBody().data().id();

        String driverEmail = unique("driver") + "@test.example";
        var driverRequest = new CreateDriverRequest("Driver", "Test", driverEmail, "password123", "9900000001",
                unique("LIC"), null, null);
        ResponseEntity<ApiResponse<DriverResponse>> driverResponse = exchange(
                "/schools/" + schoolId + "/drivers", HttpMethod.POST, driverRequest, schoolAdminToken,
                new ParameterizedTypeReference<>() {});
        assertSuccess(driverResponse);
        Long driverId = driverResponse.getBody().data().id();
        String driverToken = login(driverEmail, "password123");

        ResponseEntity<ApiResponse<RouteResponse>> assignResponse = exchange(
                "/schools/" + schoolId + "/routes/" + routeId + "/assignment", HttpMethod.PUT,
                new AssignRouteRequest(vehicleId, driverId), schoolAdminToken, new ParameterizedTypeReference<>() {});
        assertSuccess(assignResponse);

        var inviteRequest = new InviteParentRequest(unique("parent") + "@test.example", null);
        ResponseEntity<ApiResponse<InviteParentResponse>> inviteResponse = exchange(
                "/schools/" + schoolId + "/parents/invitations", HttpMethod.POST, inviteRequest, schoolAdminToken,
                new ParameterizedTypeReference<>() {});
        assertSuccess(inviteResponse);
        ResponseEntity<ApiResponse<TokenResponse>> acceptResponse = exchange(
                "/auth/invitations/" + inviteResponse.getBody().data().inviteToken() + "/accept", HttpMethod.POST,
                new AcceptInvitationRequest("password123"), null, new ParameterizedTypeReference<>() {});
        assertSuccess(acceptResponse);
        String parentToken = acceptResponse.getBody().data().accessToken();

        var createChildRequest = new CreateChildRequest("Kid", "Test", null, "3A", null, null, null, null, null);
        ResponseEntity<ApiResponse<ChildResponse>> childResponse = exchange(
                "/parents/me/children", HttpMethod.POST, createChildRequest, parentToken, new ParameterizedTypeReference<>() {});
        assertSuccess(childResponse);
        Long childId = childResponse.getBody().data().id();

        ResponseEntity<ApiResponse<ChildResponse>> stopAssignResponse = exchange(
                "/schools/" + schoolId + "/children/" + childId + "/route-stop", HttpMethod.PUT,
                new com.projectx.people.dto.AssignChildRouteStopRequest(routeId, stopId), schoolAdminToken,
                new ParameterizedTypeReference<>() {});
        assertSuccess(stopAssignResponse);

        var tripRequest = new CreateTripRequest(routeId, TripDirection.MORNING_PICKUP, LocalDate.now());
        ResponseEntity<ApiResponse<TripResponse>> tripResponse = exchange(
                "/schools/" + schoolId + "/trips", HttpMethod.POST, tripRequest, schoolAdminToken,
                new ParameterizedTypeReference<>() {});
        assertSuccess(tripResponse);
        Long tripId = tripResponse.getBody().data().id();

        ResponseEntity<ApiResponse<TripResponse>> startResponse = exchange(
                "/trips/" + tripId + "/start", HttpMethod.POST, null, driverToken, new ParameterizedTypeReference<>() {});
        assertSuccess(startResponse);

        return new Scenario(schoolId, schoolAdminToken, driverToken, parentToken, childId, tripId);
    }

    @Test
    void driverTriggersSosSchoolAdminSeesAndResolvesItButDriverCannotResolveTheirOwn() {
        Scenario s = buildScenario();

        ResponseEntity<ApiResponse<SosEventResponse>> triggerResponse = exchange(
                "/trips/" + s.tripId() + "/sos", HttpMethod.POST,
                new com.projectx.tracking.dto.TriggerSosRequest("Flat tire"), s.driverToken(),
                new ParameterizedTypeReference<>() {});
        assertSuccess(triggerResponse);
        Long sosEventId = triggerResponse.getBody().data().id();
        assertThat(triggerResponse.getBody().data().resolved()).isFalse();

        ResponseEntity<ApiResponse<List<SosEventResponse>>> activeResponse = exchange(
                "/schools/" + s.schoolId() + "/sos/active", HttpMethod.GET, null, s.schoolAdminToken(),
                new ParameterizedTypeReference<>() {});
        assertSuccess(activeResponse);
        assertThat(activeResponse.getBody().data()).extracting(SosEventResponse::id).contains(sosEventId);

        ResponseEntity<ApiResponse<SosEventResponse>> driverResolveAttempt = exchange(
                "/sos/" + sosEventId + "/resolve", HttpMethod.POST, null, s.driverToken(), new ParameterizedTypeReference<>() {});
        assertStatus(driverResolveAttempt, HttpStatus.FORBIDDEN);

        ResponseEntity<ApiResponse<SosEventResponse>> resolveResponse = exchange(
                "/sos/" + sosEventId + "/resolve", HttpMethod.POST, null, s.schoolAdminToken(), new ParameterizedTypeReference<>() {});
        assertSuccess(resolveResponse);
        assertThat(resolveResponse.getBody().data().resolved()).isTrue();

        ResponseEntity<ApiResponse<List<SosEventResponse>>> activeAfterResponse = exchange(
                "/schools/" + s.schoolId() + "/sos/active", HttpMethod.GET, null, s.schoolAdminToken(),
                new ParameterizedTypeReference<>() {});
        assertSuccess(activeAfterResponse);
        assertThat(activeAfterResponse.getBody().data()).extracting(SosEventResponse::id).doesNotContain(sosEventId);
    }

    @Test
    void parentMarkingChildAbsentExcludesThemFromTheDriversManifest() {
        Scenario s = buildScenario();

        ResponseEntity<ApiResponse<List<TripManifestEntry>>> beforeResponse = exchange(
                "/trips/" + s.tripId() + "/manifest", HttpMethod.GET, null, s.driverToken(), new ParameterizedTypeReference<>() {});
        assertSuccess(beforeResponse);
        assertThat(beforeResponse.getBody().data())
                .filteredOn(e -> e.childId().equals(s.childId()))
                .singleElement()
                .extracting(TripManifestEntry::absentToday)
                .isEqualTo(false);

        ResponseEntity<ApiResponse<Void>> absenceResponse = exchange(
                "/parents/me/children/" + s.childId() + "/absence", HttpMethod.PUT,
                new com.projectx.tracking.dto.SetChildAbsenceRequest(LocalDate.now()), s.parentToken(),
                new ParameterizedTypeReference<>() {});
        assertSuccess(absenceResponse);

        ResponseEntity<ApiResponse<List<TripManifestEntry>>> afterResponse = exchange(
                "/trips/" + s.tripId() + "/manifest", HttpMethod.GET, null, s.driverToken(), new ParameterizedTypeReference<>() {});
        assertSuccess(afterResponse);
        assertThat(afterResponse.getBody().data())
                .filteredOn(e -> e.childId().equals(s.childId()))
                .singleElement()
                .extracting(TripManifestEntry::absentToday)
                .isEqualTo(true);
    }

    @Test
    void driverScansChildQrCodeToRecordBoardingAndATamperedTokenIsRejected() {
        Scenario s = buildScenario();

        ResponseEntity<ApiResponse<String>> qrResponse = exchange(
                "/parents/me/children/" + s.childId() + "/qr-token", HttpMethod.GET, null, s.parentToken(),
                new ParameterizedTypeReference<>() {});
        assertSuccess(qrResponse);
        String qrToken = qrResponse.getBody().data();

        var scanRequest = new com.projectx.tracking.dto.RecordScannedTripEventRequest(qrToken, ChildTripEventType.BOARDED);
        ResponseEntity<ApiResponse<com.projectx.tracking.dto.ChildTripEventResponse>> scanResponse = exchange(
                "/trips/" + s.tripId() + "/child-events/scan", HttpMethod.POST, scanRequest, s.driverToken(),
                new ParameterizedTypeReference<>() {});
        assertSuccess(scanResponse);
        assertThat(scanResponse.getBody().data().eventType()).isEqualTo(ChildTripEventType.BOARDED);

        var tamperedRequest = new com.projectx.tracking.dto.RecordScannedTripEventRequest(
                qrToken.substring(0, qrToken.length() - 2) + "xx", ChildTripEventType.BOARDED);
        ResponseEntity<ApiResponse<com.projectx.tracking.dto.ChildTripEventResponse>> tamperedResponse = exchange(
                "/trips/" + s.tripId() + "/child-events/scan", HttpMethod.POST, tamperedRequest, s.driverToken(),
                new ParameterizedTypeReference<>() {});
        assertStatus(tamperedResponse, HttpStatus.BAD_REQUEST);
    }
}

package com.projectx.app;

import static org.assertj.core.api.Assertions.assertThat;

import com.projectx.common.web.ApiResponse;
import com.projectx.identity.dto.AcceptInvitationRequest;
import com.projectx.identity.dto.TokenResponse;
import com.projectx.people.dto.ChildResponse;
import com.projectx.people.dto.CompleteParentProfileRequest;
import com.projectx.people.dto.CreateChildRequest;
import com.projectx.people.dto.InviteParentRequest;
import com.projectx.people.dto.InviteParentResponse;
import com.projectx.people.dto.ParentResponse;
import com.projectx.school.dto.CreateRouteRequest;
import com.projectx.school.dto.CreateSchoolRequest;
import com.projectx.school.dto.RouteResponse;
import com.projectx.school.dto.RouteStopRequest;
import com.projectx.school.dto.SchoolResponse;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;

/**
 * Zone-based auto-allocation is the newest and most novel piece of this domain: a child gets
 * assigned to whichever route's [zoneMin, zoneMax) distance-from-school band covers their home,
 * then the nearest stop on that route. This exercises the whole chain end to end - school
 * location, route zone config, parent home location, child creation, then the algorithm itself.
 */
class ChildAutoAssignRouteTest extends IntegrationTestBase {

    private static final double SCHOOL_LAT = 18.5204;
    private static final double SCHOOL_LNG = 73.8567;
    // Roughly 3km from the school coordinates above - falls inside a 0-6000m zone.
    private static final double HOME_LAT = 18.545;
    private static final double HOME_LNG = 73.87;

    @Test
    void childIsAutoAssignedToTheRouteWhoseZoneCoversTheirHomeDistance() {
        String platformToken = platformAdminToken();

        String schoolAdminEmail = unique("schooladmin") + "@test.example";
        var createSchoolRequest = new CreateSchoolRequest(unique("Zone School "), "1 Test Rd", "Pune", null, null,
                null, null, SCHOOL_LAT, SCHOOL_LNG, 300, null, "Admin", "User", schoolAdminEmail, "password123");
        ResponseEntity<ApiResponse<SchoolResponse>> schoolResponse = exchange(
                "/schools", HttpMethod.POST, createSchoolRequest, platformToken, new ParameterizedTypeReference<>() {});
        assertSuccess(schoolResponse);
        Long schoolId = schoolResponse.getBody().data().id();
        String schoolAdminToken = login(schoolAdminEmail, "password123");

        var routeRequest = new CreateRouteRequest(unique("Zone A "),
                List.of(new RouteStopRequest(1, "Main Gate", HOME_LAT, HOME_LNG, 150)), 0, 6000);
        ResponseEntity<ApiResponse<RouteResponse>> routeResponse = exchange(
                "/schools/" + schoolId + "/routes", HttpMethod.POST, routeRequest, schoolAdminToken,
                new ParameterizedTypeReference<>() {});
        assertSuccess(routeResponse);
        Long routeId = routeResponse.getBody().data().id();
        Long stopId = routeResponse.getBody().data().stops().get(0).id();

        var inviteRequest = new InviteParentRequest(unique("parent") + "@test.example", null);
        ResponseEntity<ApiResponse<InviteParentResponse>> inviteResponse = exchange(
                "/schools/" + schoolId + "/parents/invitations", HttpMethod.POST, inviteRequest, schoolAdminToken,
                new ParameterizedTypeReference<>() {});
        assertSuccess(inviteResponse);
        String inviteToken = inviteResponse.getBody().data().inviteToken();

        ResponseEntity<ApiResponse<TokenResponse>> acceptResponse = exchange(
                "/auth/invitations/" + inviteToken + "/accept", HttpMethod.POST,
                new AcceptInvitationRequest("password123"), null, new ParameterizedTypeReference<>() {});
        assertSuccess(acceptResponse);
        String parentToken = acceptResponse.getBody().data().accessToken();

        var completeProfileRequest = new CompleteParentProfileRequest("Parent", "Test", "9900000000", "1 Home St",
                HOME_LAT, HOME_LNG);
        ResponseEntity<ApiResponse<ParentResponse>> profileResponse = exchange(
                "/parents/me", HttpMethod.PATCH, completeProfileRequest, parentToken, new ParameterizedTypeReference<>() {});
        assertSuccess(profileResponse);

        var createChildRequest = new CreateChildRequest("Child", "Test", null, "1A", null);
        ResponseEntity<ApiResponse<ChildResponse>> childResponse = exchange(
                "/parents/me/children", HttpMethod.POST, createChildRequest, parentToken, new ParameterizedTypeReference<>() {});
        assertSuccess(childResponse);
        Long childId = childResponse.getBody().data().id();
        assertThat(childResponse.getBody().data().routeId()).isNull();

        ResponseEntity<ApiResponse<ChildResponse>> autoAssignResponse = exchange(
                "/schools/" + schoolId + "/children/" + childId + "/auto-assign-route", HttpMethod.POST, null,
                schoolAdminToken, new ParameterizedTypeReference<>() {});
        assertSuccess(autoAssignResponse);
        assertThat(autoAssignResponse.getBody().data().routeId()).isEqualTo(routeId);
        assertThat(autoAssignResponse.getBody().data().routeStopId()).isEqualTo(stopId);
    }
}

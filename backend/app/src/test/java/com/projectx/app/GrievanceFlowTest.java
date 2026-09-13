package com.projectx.app;

import static org.assertj.core.api.Assertions.assertThat;

import com.projectx.common.web.ApiResponse;
import com.projectx.grievance.domain.GrievanceCategory;
import com.projectx.grievance.domain.GrievanceStatus;
import com.projectx.grievance.dto.CreateGrievanceRequest;
import com.projectx.grievance.dto.GrievanceResponse;
import com.projectx.grievance.dto.UpdateGrievanceStatusRequest;
import com.projectx.identity.dto.AcceptInvitationRequest;
import com.projectx.identity.dto.TokenResponse;
import com.projectx.people.dto.InviteParentRequest;
import com.projectx.people.dto.InviteParentResponse;
import com.projectx.school.dto.CreateSchoolRequest;
import com.projectx.school.dto.SchoolResponse;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

class GrievanceFlowTest extends IntegrationTestBase {

    private record CreatedSchool(SchoolResponse school, String adminEmail) {}

    private CreatedSchool createSchool(String platformToken) {
        String adminEmail = unique("schooladmin") + "@test.example";
        var request = new CreateSchoolRequest(unique("Grievance School "), "1 Test Rd", "Pune", null, null, null,
                null, null, null, null, null, null, "Admin", "User", adminEmail, "password123");
        ResponseEntity<ApiResponse<SchoolResponse>> response = exchange(
                "/schools", HttpMethod.POST, request, platformToken, new ParameterizedTypeReference<>() {});
        assertSuccess(response);
        return new CreatedSchool(response.getBody().data(), adminEmail);
    }

    private String inviteAndAcceptParent(Long schoolId, String schoolAdminToken) {
        var inviteRequest = new InviteParentRequest(unique("parent") + "@test.example", null);
        ResponseEntity<ApiResponse<InviteParentResponse>> inviteResponse = exchange(
                "/schools/" + schoolId + "/parents/invitations", HttpMethod.POST, inviteRequest, schoolAdminToken,
                new ParameterizedTypeReference<>() {});
        assertSuccess(inviteResponse);

        ResponseEntity<ApiResponse<TokenResponse>> acceptResponse = exchange(
                "/auth/invitations/" + inviteResponse.getBody().data().inviteToken() + "/accept", HttpMethod.POST,
                new AcceptInvitationRequest("password123"), null, new ParameterizedTypeReference<>() {});
        assertSuccess(acceptResponse);
        return acceptResponse.getBody().data().accessToken();
    }

    @Test
    void parentRaisesGrievanceSchoolResolvesItAndPlatformAdminSeesItEverywhere() {
        String platformToken = platformAdminToken();
        CreatedSchool school = createSchool(platformToken);
        String schoolAdminToken = login(school.adminEmail(), "password123");
        String parentToken = inviteAndAcceptParent(school.school().id(), schoolAdminToken);

        var createRequest = new CreateGrievanceRequest(school.school().id(), GrievanceCategory.DELAY,
                "Bus was late", "The bus arrived 30 minutes late with no notice.");
        ResponseEntity<ApiResponse<GrievanceResponse>> raiseResponse = exchange(
                "/grievances", HttpMethod.POST, createRequest, parentToken, new ParameterizedTypeReference<>() {});
        assertSuccess(raiseResponse);
        Long grievanceId = raiseResponse.getBody().data().id();
        assertThat(raiseResponse.getBody().data().status()).isEqualTo(GrievanceStatus.OPEN);

        ResponseEntity<ApiResponse<List<GrievanceResponse>>> mineResponse = exchange(
                "/grievances/mine", HttpMethod.GET, null, parentToken, new ParameterizedTypeReference<>() {});
        assertSuccess(mineResponse);
        assertThat(mineResponse.getBody().data()).extracting(GrievanceResponse::id).contains(grievanceId);

        ResponseEntity<ApiResponse<List<GrievanceResponse>>> schoolListResponse = exchange(
                "/schools/" + school.school().id() + "/grievances", HttpMethod.GET, null, schoolAdminToken,
                new ParameterizedTypeReference<>() {});
        assertSuccess(schoolListResponse);
        assertThat(schoolListResponse.getBody().data()).extracting(GrievanceResponse::id).contains(grievanceId);

        var resolveRequest = new UpdateGrievanceStatusRequest(GrievanceStatus.RESOLVED, "Spoke to the driver.");
        ResponseEntity<ApiResponse<GrievanceResponse>> resolveResponse = exchange(
                "/grievances/" + grievanceId + "/status", HttpMethod.PATCH, resolveRequest, schoolAdminToken,
                new ParameterizedTypeReference<>() {});
        assertSuccess(resolveResponse);
        assertThat(resolveResponse.getBody().data().status()).isEqualTo(GrievanceStatus.RESOLVED);

        ResponseEntity<ApiResponse<List<GrievanceResponse>>> allResponse = exchange(
                "/grievances", HttpMethod.GET, null, platformToken, new ParameterizedTypeReference<>() {});
        assertSuccess(allResponse);
        assertThat(allResponse.getBody().data())
                .filteredOn(g -> g.id().equals(grievanceId))
                .singleElement()
                .extracting(GrievanceResponse::status)
                .isEqualTo(GrievanceStatus.RESOLVED);
    }

    @Test
    void anotherSchoolsAdminCannotResolveThisSchoolsGrievance() {
        String platformToken = platformAdminToken();
        CreatedSchool schoolA = createSchool(platformToken);
        CreatedSchool schoolB = createSchool(platformToken);
        String schoolAAdminToken = login(schoolA.adminEmail(), "password123");
        String schoolBAdminToken = login(schoolB.adminEmail(), "password123");
        String parentToken = inviteAndAcceptParent(schoolA.school().id(), schoolAAdminToken);

        var createRequest = new CreateGrievanceRequest(schoolA.school().id(), GrievanceCategory.SAFETY,
                "Unsafe driving", "Driver ran a red light.");
        ResponseEntity<ApiResponse<GrievanceResponse>> raiseResponse = exchange(
                "/grievances", HttpMethod.POST, createRequest, parentToken, new ParameterizedTypeReference<>() {});
        assertSuccess(raiseResponse);
        Long grievanceId = raiseResponse.getBody().data().id();

        var resolveRequest = new UpdateGrievanceStatusRequest(GrievanceStatus.REJECTED, "Not my school.");
        ResponseEntity<ApiResponse<GrievanceResponse>> resolveResponse = exchange(
                "/grievances/" + grievanceId + "/status", HttpMethod.PATCH, resolveRequest, schoolBAdminToken,
                new ParameterizedTypeReference<>() {});
        assertStatus(resolveResponse, HttpStatus.FORBIDDEN);
    }
}

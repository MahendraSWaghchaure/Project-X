package com.projectx.app;

import static org.assertj.core.api.Assertions.assertThat;

import com.projectx.common.web.ApiResponse;
import com.projectx.school.domain.SchoolStatus;
import com.projectx.school.dto.CreateSchoolRequest;
import com.projectx.school.dto.SchoolResponse;
import com.projectx.school.dto.UpdateSchoolRequest;
import com.projectx.school.dto.UpdateSchoolStatusRequest;
import org.junit.jupiter.api.Test;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

class SchoolLifecycleTest extends IntegrationTestBase {

    private record CreatedSchool(SchoolResponse school, String adminEmail, String adminPassword) {}

    private CreatedSchool createSchool(String platformToken, String namePrefix) {
        String adminEmail = unique(namePrefix.toLowerCase()) + "@test.example";
        String adminPassword = "password123";
        var request = new CreateSchoolRequest(namePrefix, "1 Test Rd", "Pune", null, null, null, null,
                null, null, null, null, null, "Admin", "User", adminEmail, adminPassword);
        ResponseEntity<ApiResponse<SchoolResponse>> response = exchange(
                "/schools", HttpMethod.POST, request, platformToken, new ParameterizedTypeReference<>() {});
        assertSuccess(response);
        return new CreatedSchool(response.getBody().data(), adminEmail, adminPassword);
    }

    @Test
    void platformAdminCanCreateEditAndSuspendASchool() {
        String platformToken = platformAdminToken();
        SchoolResponse school = createSchool(platformToken, unique("EditMe")).school();
        assertThat(school.status()).isEqualTo(SchoolStatus.ACTIVE);

        var updateRequest = new UpdateSchoolRequest("Renamed School", "2 New Rd", "Mumbai", "MH", "400001", null,
                "9999999999", "contact@school.example", null, null, null, null);
        ResponseEntity<ApiResponse<SchoolResponse>> updateResponse = exchange(
                "/schools/" + school.id(), HttpMethod.PATCH, updateRequest, platformToken,
                new ParameterizedTypeReference<>() {});
        assertSuccess(updateResponse);
        assertThat(updateResponse.getBody().data().name()).isEqualTo("Renamed School");

        ResponseEntity<ApiResponse<SchoolResponse>> suspendResponse = exchange(
                "/schools/" + school.id() + "/status", HttpMethod.PATCH, new UpdateSchoolStatusRequest(SchoolStatus.SUSPENDED, "Fees overdue"),
                platformToken, new ParameterizedTypeReference<>() {});
        assertSuccess(suspendResponse);
        assertThat(suspendResponse.getBody().data().status()).isEqualTo(SchoolStatus.SUSPENDED);
        assertThat(suspendResponse.getBody().data().statusReason()).isEqualTo("Fees overdue");
    }

    @Test
    void schoolAdminCannotReadAnotherSchoolsVehicles() {
        String platformToken = platformAdminToken();
        CreatedSchool schoolA = createSchool(platformToken, unique("SchoolA"));
        CreatedSchool schoolB = createSchool(platformToken, unique("SchoolB"));
        String schoolBAdminToken = login(schoolB.adminEmail(), schoolB.adminPassword());

        ResponseEntity<ApiResponse<Object>> ownSchoolResponse = exchange(
                "/schools/" + schoolB.school().id() + "/vehicles", HttpMethod.GET, null, schoolBAdminToken,
                new ParameterizedTypeReference<>() {});
        assertSuccess(ownSchoolResponse);

        ResponseEntity<ApiResponse<Object>> otherSchoolResponse = exchange(
                "/schools/" + schoolA.school().id() + "/vehicles", HttpMethod.GET, null, schoolBAdminToken,
                new ParameterizedTypeReference<>() {});
        assertStatus(otherSchoolResponse, HttpStatus.FORBIDDEN);
    }
}

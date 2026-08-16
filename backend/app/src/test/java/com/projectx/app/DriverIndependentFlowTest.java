package com.projectx.app;

import static org.assertj.core.api.Assertions.assertThat;

import com.projectx.common.web.ApiResponse;
import com.projectx.people.dto.DriverResponse;
import com.projectx.people.dto.DriverSelfRegisterRequest;
import com.projectx.school.domain.VehicleType;
import com.projectx.school.dto.CreateSchoolRequest;
import com.projectx.school.dto.SchoolResponse;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;

class DriverIndependentFlowTest extends IntegrationTestBase {

    private record CreatedSchool(SchoolResponse school, String adminEmail) {}

    private CreatedSchool createSchool(String platformToken) {
        String adminEmail = unique("schooladmin") + "@test.example";
        var request = new CreateSchoolRequest(unique("Test School "), "1 Test Rd", "Pune", null, null, null, null,
                null, null, null, null, "Admin", "User", adminEmail, "password123");
        ResponseEntity<ApiResponse<SchoolResponse>> response = exchange(
                "/schools", HttpMethod.POST, request, platformToken, new ParameterizedTypeReference<>() {});
        assertSuccess(response);
        return new CreatedSchool(response.getBody().data(), adminEmail);
    }

    @Test
    void selfRegisteredDriverAppearsInIndependentPoolAndCanBeClaimedByASchool() {
        String platformToken = platformAdminToken();
        String driverEmail = unique("indriver") + "@test.example";
        var registerRequest = new DriverSelfRegisterRequest("Indie", "Driver", driverEmail, "password123",
                "9876500000", unique("LIC-"), null, unique("REG").toUpperCase(), VehicleType.VAN, 10);

        ResponseEntity<ApiResponse<DriverResponse>> registerResponse = exchange(
                "/drivers/register", HttpMethod.POST, registerRequest, null, new ParameterizedTypeReference<>() {});
        assertSuccess(registerResponse);
        DriverResponse driver = registerResponse.getBody().data();
        assertThat(driver.schoolId()).isNull();
        assertThat(driver.vehicleId()).isNotNull();

        ResponseEntity<ApiResponse<List<DriverResponse>>> poolResponse = exchange(
                "/drivers/independent", HttpMethod.GET, null, platformToken, new ParameterizedTypeReference<>() {});
        assertSuccess(poolResponse);
        assertThat(poolResponse.getBody().data()).extracting(DriverResponse::id).contains(driver.id());

        CreatedSchool created = createSchool(platformToken);
        String schoolAdminToken = login(created.adminEmail(), "password123");
        ResponseEntity<ApiResponse<DriverResponse>> claimResponse = exchange(
                "/schools/" + created.school().id() + "/drivers/" + driver.id() + "/claim", HttpMethod.POST, null,
                schoolAdminToken, new ParameterizedTypeReference<>() {});
        assertSuccess(claimResponse);
        assertThat(claimResponse.getBody().data().schoolId()).isEqualTo(created.school().id());

        ResponseEntity<ApiResponse<List<DriverResponse>>> poolAfterClaim = exchange(
                "/drivers/independent", HttpMethod.GET, null, platformToken, new ParameterizedTypeReference<>() {});
        assertSuccess(poolAfterClaim);
        assertThat(poolAfterClaim.getBody().data()).extracting(DriverResponse::id).doesNotContain(driver.id());

        ResponseEntity<ApiResponse<List<DriverResponse>>> schoolRoster = exchange(
                "/schools/" + created.school().id() + "/drivers", HttpMethod.GET, null, schoolAdminToken,
                new ParameterizedTypeReference<>() {});
        assertSuccess(schoolRoster);
        assertThat(schoolRoster.getBody().data()).extracting(DriverResponse::id).contains(driver.id());
    }
}

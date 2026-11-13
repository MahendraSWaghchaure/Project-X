package com.projectx.app;

import static org.assertj.core.api.Assertions.assertThat;

import com.projectx.common.web.ApiResponse;
import com.projectx.people.dto.CreateDriverRequest;
import com.projectx.people.dto.DriverResponse;
import com.projectx.people.dto.DriverSelfRegisterRequest;
import com.projectx.school.domain.VehicleType;
import com.projectx.school.dto.CreateSchoolRequest;
import com.projectx.school.dto.SchoolResponse;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

class DriverIndependentFlowTest extends IntegrationTestBase {

    private record CreatedSchool(SchoolResponse school, String adminEmail) {}

    private CreatedSchool createSchool(String platformToken) {
        String adminEmail = unique("schooladmin") + "@test.example";
        var request = new CreateSchoolRequest(unique("Test School "), "1 Test Rd", "Pune", null, null, null, null,
                null, null, null, null, null, "Admin", "User", adminEmail, "password123");
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

    /**
     * Regression test: a school-created driver (the normal {@code POST /schools/{id}/drivers} flow,
     * as opposed to self-register+claim) never has a vehicleId, so every entry in the roster maps to
     * a null vehicleId. DriverService.vehicleNumbers() used to build that no-vehicles lookup map with
     * Map.of() for the empty-ids case, and an immutable Map throws NullPointerException on get(null)
     * regardless of whether the map has entries - which 500'd this endpoint for any school whose
     * drivers all lack an allocated vehicle (the common case). See DriverService#vehicleNumbers.
     */
    @Test
    void listDriversSucceedsWhenNoDriverHasAnAllocatedVehicle() {
        String platformToken = platformAdminToken();
        CreatedSchool created = createSchool(platformToken);
        String schoolAdminToken = login(created.adminEmail(), "password123");

        var createRequest = new CreateDriverRequest("No", "Vehicle", unique("nodriver") + "@test.example",
                "password123", "9876500001", unique("LIC-"), null, null);
        ResponseEntity<ApiResponse<DriverResponse>> createResponse = exchange(
                "/schools/" + created.school().id() + "/drivers", HttpMethod.POST, createRequest, schoolAdminToken,
                new ParameterizedTypeReference<>() {});
        assertStatus(createResponse, HttpStatus.CREATED);
        DriverResponse createdDriver = createResponse.getBody().data();
        assertThat(createdDriver.vehicleId()).isNull();

        ResponseEntity<ApiResponse<List<DriverResponse>>> schoolRoster = exchange(
                "/schools/" + created.school().id() + "/drivers", HttpMethod.GET, null, schoolAdminToken,
                new ParameterizedTypeReference<>() {});
        assertSuccess(schoolRoster);
        assertThat(schoolRoster.getBody().data()).extracting(DriverResponse::id).contains(createdDriver.id());
        assertThat(schoolRoster.getBody().data()).extracting(DriverResponse::vehicleRegistrationNumber)
                .allMatch(java.util.Objects::isNull);
    }
}

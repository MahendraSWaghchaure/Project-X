package com.projectx.app;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.projectx.attendance.domain.ArrivalKind;
import com.projectx.attendance.domain.ParentResponse;
import com.projectx.attendance.dto.ArrivalDtos.ArrivalResponse;
import com.projectx.attendance.dto.ArrivalDtos.RespondRequest;
import com.projectx.common.events.ChildTripEventType;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;

/**
 * Real STOMP-over-WebSocket tests (actual socket, JWT on CONNECT, SUBSCRIBE + receipt, pushed frames):
 * the arrival topics must accept the right principals, reject everyone else (including wildcard
 * patterns that the simple broker would otherwise expand to other tenants' topics), and push
 * ISO-8601 timestamps with no parentNote on the driver-visible trip topic.
 */
class SchoolAttendanceStompTest extends SchoolAttendanceTestKit {

    private static final ObjectMapper JSON = new ObjectMapper();

    private boolean canSubscribe(String jwt, String destination) throws Exception {
        try (StompProbe probe = StompProbe.connect(port, jwt)) {
            return probe.subscribe(destination);
        }
    }

    @Test
    void connectSubscribeOwnTopicsOkOthersRejectedAndFramesArrive() throws Exception {
        Env a = buildEnv();
        Env b = buildEnv();
        String parentTopic = "/topic/parent/" + a.parentUserId() + "/arrivals";
        String schoolTopic = "/topic/school/" + a.schoolId() + "/arrivals";
        String tripTopic = "/topic/trip/" + a.tripId() + "/arrivals";

        // Own topics are accepted.
        assertThat(canSubscribe(a.parentToken(), parentTopic)).as("parent own topic").isTrue();
        assertThat(canSubscribe(a.adminToken(), schoolTopic)).as("admin own school topic").isTrue();
        // Phase 2: the school-wide arrival board is admin-only; a teacher gets their own class alerts topic instead.
        assertThat(canSubscribe(a.teacherToken(), schoolTopic)).as("teacher own school topic").isFalse();
        assertThat(canSubscribe(a.driverToken(), tripTopic)).as("assigned driver own trip topic").isTrue();
        assertThat(canSubscribe(a.adminToken(), tripTopic)).as("school admin trip topic").isTrue();

        // Everyone else is rejected.
        assertThat(canSubscribe(b.parentToken(), parentTopic)).as("other parent -> parent topic").isFalse();
        assertThat(canSubscribe(a.adminToken(), parentTopic)).as("admin -> parent topic").isFalse();
        assertThat(canSubscribe(a.parentToken(), schoolTopic)).as("parent -> school topic").isFalse();
        assertThat(canSubscribe(a.driverToken(), schoolTopic)).as("driver -> school topic").isFalse();
        assertThat(canSubscribe(b.adminToken(), schoolTopic)).as("other school admin -> school topic").isFalse();
        assertThat(canSubscribe(a.parentToken(), tripTopic)).as("parent -> trip topic").isFalse();
        assertThat(canSubscribe(b.adminToken(), tripTopic)).as("other school admin -> trip topic").isFalse();
        assertThat(canSubscribe(b.driverToken(), tripTopic)).as("other school driver -> trip topic").isFalse();

        // A frame published by the real flow reaches the authorised subscribers.
        Long child = a.childIds().get(0);
        subscribed(a, "StompKid", child);
        try (StompProbe parent = StompProbe.connect(port, a.parentToken());
             StompProbe school = StompProbe.connect(port, a.adminToken());
             StompProbe trip = StompProbe.connect(port, a.driverToken())) {
            assertThat(parent.subscribe(parentTopic)).isTrue();
            assertThat(school.subscribe(schoolTopic)).isTrue();
            assertThat(trip.subscribe(tripTopic)).isTrue();

            assertSuccess(driverEvent(a, child, ChildTripEventType.DROPPED_AT_SCHOOL));

            for (StompProbe p : List.of(parent, school, trip)) {
                String frame = p.poll(10);
                assertThat(frame).as("pushed frame").isNotNull();
                JsonNode node = JSON.readTree(frame);
                assertThat(node.get("childId").asLong()).isEqualTo(child);
                assertThat(node.get("kind").asText()).isEqualTo(ArrivalKind.SCHOOL_DROP.name());
                // ISO-8601 strings exactly like REST - never epoch-second numbers.
                assertThat(node.get("driverEventAt").isTextual()).as("driverEventAt is text: %s", node.get("driverEventAt")).isTrue();
                assertThat(node.get("updatedAt").isTextual()).as("updatedAt is text: %s", node.get("updatedAt")).isTrue();
                assertThat(java.time.Instant.parse(node.get("driverEventAt").asText())).isNotNull();
            }
        }
    }

    @Test
    void wildcardAndMalformedSubscriptionsAreRejected() throws Exception {
        Env a = buildEnv();
        for (String bad : List.of(
                "/topic/school/*/arrivals",
                "/topic/trip/*/arrivals",
                "/topic/parent/*/arrivals",
                "/topic/**",
                "/topic/school/**",
                "/topic/school/" + a.schoolId() + "/**",
                "/topic/school/{id}/arrivals",
                "/topic/school/" + a.schoolId() + "/arrivals/",
                "/topic/school/" + a.schoolId() + "/arrivals/x",
                "/topic/school/0" + a.schoolId() + "x/arrivals",
                "/topic/school/?/arrivals",
                "/topic/trip//arrivals",
                "/topic/trip/" + a.tripId() + "/../" + a.tripId() + "/arrivals",
                "/topic/trip/" + a.tripId() + "/arrival?")) {
            // Even the school admin / platform admin must not be able to use a pattern.
            assertThat(canSubscribe(a.adminToken(), bad)).as("admin subscribe %s", bad).isFalse();
            assertThat(canSubscribe(platformAdminToken(), bad)).as("platform admin subscribe %s", bad).isFalse();
        }
        // Anonymous (no token on CONNECT) cannot even connect.
        try {
            StompProbe probe = StompProbe.connect(port, null);
            if (probe != null) {
                try (probe) {
                    assertThat(probe.subscribe("/topic/school/" + a.schoolId() + "/arrivals")).isFalse();
                }
            }
        } catch (Exception expected) {
            // rejected at CONNECT - also fine
        }
    }

    @Test
    void parentNoteReachesSchoolAndParentButNeverTheDriverTopicOrDriverRest() throws Exception {
        Env a = buildEnv();
        Long child = a.childIds().get(0);
        subscribed(a, "NoteKid", child);
        assertSuccess(driverEvent(a, child, ChildTripEventType.DROPPED_AT_SCHOOL));
        ArrivalResponse arrival = awaitArrival(a, child, ArrivalKind.SCHOOL_DROP);

        try (StompProbe school = StompProbe.connect(port, a.adminToken());
             StompProbe trip = StompProbe.connect(port, a.driverToken())) {
            assertThat(school.subscribe("/topic/school/" + a.schoolId() + "/arrivals")).isTrue();
            assertThat(trip.subscribe("/topic/trip/" + a.tripId() + "/arrivals")).isTrue();

            ok(this.<ArrivalResponse>exchange("/parents/me/arrivals/" + arrival.id() + "/respond", HttpMethod.POST,
                    new RespondRequest(ParentResponse.DISPUTED, "private: ask for the other guardian"), a.parentToken(),
                    new ParameterizedTypeReference<>() {}));

            String schoolFrame = school.poll(10);
            String tripFrame = trip.poll(10);
            assertThat(schoolFrame).isNotNull().contains("private: ask for the other guardian");
            assertThat(tripFrame).isNotNull().doesNotContain("private: ask for the other guardian");
            JsonNode tripNode = JSON.readTree(tripFrame);
            assertThat(tripNode.get("parentResponse").asText()).isEqualTo("DISPUTED");
            assertThat(tripNode.path("parentNote").isMissingNode() || tripNode.get("parentNote").isNull()).isTrue();
        }

        // REST for the driver: no note; for the school admin: note kept.
        assertThat(tripArrivals(a, a.driverToken())).filteredOn(x -> x.id().equals(arrival.id()))
                .singleElement().extracting(ArrivalResponse::parentNote).isNull();
        assertThat(tripArrivals(a, a.adminToken())).filteredOn(x -> x.id().equals(arrival.id()))
                .singleElement().extracting(ArrivalResponse::parentNote).isEqualTo("private: ask for the other guardian");
    }
}

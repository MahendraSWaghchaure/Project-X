package com.projectx.attendance.dto;

import com.projectx.attendance.domain.Arrival;
import com.projectx.attendance.domain.ArrivalKind;
import com.projectx.attendance.domain.ArrivalOutcome;
import com.projectx.attendance.domain.ParentResponse;
import com.projectx.attendance.domain.SchoolConfirm;
import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;

public final class ArrivalDtos {

    private ArrivalDtos() {
    }

    /**
     * Contract fields first; {@code schoolId} and {@code parentNote} are additive extras. The two instants are
     * pinned to ISO-8601 strings so the REST body and the STOMP frames pushed through the Redis relay (whose
     * shared ObjectMapper writes epoch numbers by default) look identical to clients.
     */
    public record ArrivalResponse(Long id, Long tripId, Long childId, Long studentId, String childName, ArrivalKind kind,
                                  @JsonFormat(shape = JsonFormat.Shape.STRING) Instant driverEventAt,
                                  SchoolConfirm schoolConfirm, ParentResponse parentResponse,
                                  ArrivalOutcome outcome, @JsonFormat(shape = JsonFormat.Shape.STRING) Instant updatedAt,
                                  Long schoolId, String parentNote) {

        /** The driver must never see what a guardian wrote to the school (it can be a private safety concern). */
        public ArrivalResponse withoutParentNote() {
            return new ArrivalResponse(id, tripId, childId, studentId, childName, kind, driverEventAt, schoolConfirm,
                    parentResponse, outcome, updatedAt, schoolId, null);
        }

        public static ArrivalResponse from(Arrival a) {
            return new ArrivalResponse(a.getId(), a.getTripId(), a.getChildId(), a.getStudentId(), a.getChildName(),
                    a.getKind(), a.getDriverEventAt(), a.getSchoolConfirm(), a.getParentResponse(), a.getOutcome(),
                    a.getUpdatedAt(), a.getSchoolId(), a.getParentNote());
        }
    }

    /** response must be APPROVED or DISPUTED - PENDING is not something a parent can "respond" with. */
    public record RespondRequest(@NotNull ParentResponse response, @Size(max = 500) String note) {
    }
}

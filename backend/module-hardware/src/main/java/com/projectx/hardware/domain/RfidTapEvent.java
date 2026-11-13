package com.projectx.hardware.domain;

import com.projectx.common.domain.Auditable;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

/**
 * One raw RFID/NFC gate-tap sample as reported by a device - {@code cardToken} is whatever the
 * card/tag itself encodes, stored verbatim; this module does not resolve it to a child, a trip, or
 * a safe-arrival decision (see HardwareIngestController's javadoc - that wiring, if it ever
 * happens, belongs to module-attendance/module-tracking, not here).
 *
 * <p>Idempotent on {@code (device_id, event_timestamp)} (see V110 migration's unique index) - a
 * flaky device retrying the same tap after a dropped response does not create a second row.
 */
@Entity
@Table(name = "rfid_tap_events")
public class RfidTapEvent extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "device_id", nullable = false, length = 100)
    private String deviceId;

    @Column(name = "card_token", nullable = false, length = 200)
    private String cardToken;

    @Enumerated(EnumType.STRING)
    @Column(name = "gate_type", nullable = false, length = 16)
    private GateType gateType;

    /** The device's own clock at the moment of the tap - the natural key, together with deviceId, for idempotency. */
    @Column(name = "event_timestamp", nullable = false)
    private Instant eventTimestamp;

    protected RfidTapEvent() {
    }

    public RfidTapEvent(String deviceId, String cardToken, GateType gateType, Instant eventTimestamp) {
        this.deviceId = deviceId;
        this.cardToken = cardToken;
        this.gateType = gateType;
        this.eventTimestamp = eventTimestamp;
    }

    public Long getId() {
        return id;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public String getCardToken() {
        return cardToken;
    }

    public GateType getGateType() {
        return gateType;
    }

    public Instant getEventTimestamp() {
        return eventTimestamp;
    }
}

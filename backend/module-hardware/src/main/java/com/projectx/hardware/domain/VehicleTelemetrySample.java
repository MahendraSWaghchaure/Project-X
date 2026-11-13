package com.projectx.hardware.domain;

import com.projectx.common.domain.Auditable;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

/**
 * One raw vehicle telemetry sample (speed/harsh-braking/door-open) as reported by an in-vehicle
 * device - pure data capture, not wired into module-tracking's existing GPS-ping safety pipeline
 * (speed-limit/harsh-braking checks already live there, driven by Traccar pings - see
 * HardwareIngestController's javadoc for why this module doesn't touch that).
 *
 * <p>Idempotent on {@code (device_id, sample_timestamp)} (see V110 migration's unique index).
 */
@Entity
@Table(name = "vehicle_telemetry_samples")
public class VehicleTelemetrySample extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "device_id", nullable = false, length = 100)
    private String deviceId;

    @Column(name = "speed_kmh")
    private Double speedKmh;

    @Column(name = "harsh_braking", nullable = false)
    private boolean harshBraking;

    @Column(name = "door_open", nullable = false)
    private boolean doorOpen;

    /** The device's own clock at the moment of the sample - the natural key, together with deviceId, for idempotency. */
    @Column(name = "sample_timestamp", nullable = false)
    private Instant sampleTimestamp;

    protected VehicleTelemetrySample() {
    }

    public VehicleTelemetrySample(String deviceId, Double speedKmh, boolean harshBraking, boolean doorOpen, Instant sampleTimestamp) {
        this.deviceId = deviceId;
        this.speedKmh = speedKmh;
        this.harshBraking = harshBraking;
        this.doorOpen = doorOpen;
        this.sampleTimestamp = sampleTimestamp;
    }

    public Long getId() {
        return id;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public Double getSpeedKmh() {
        return speedKmh;
    }

    public boolean isHarshBraking() {
        return harshBraking;
    }

    public boolean isDoorOpen() {
        return doorOpen;
    }

    public Instant getSampleTimestamp() {
        return sampleTimestamp;
    }
}

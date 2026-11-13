package com.projectx.hardware.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** Per-device-id request caps for the hardware ingestion endpoints - see application.yml for the defaults actually wired in. */
@ConfigurationProperties(prefix = "projectx.hardware.rate-limit")
public class HardwareRateLimitProperties {

    /** RFID/NFC taps are human-paced events - generous headroom for retries, not a GPS-style firehose. */
    private int tapPerMinute = 60;

    /** Vehicle telemetry is the higher-frequency one (closer to the ~10s GPS ping cadence elsewhere in the system). */
    private int telemetryPerMinute = 120;

    public int getTapPerMinute() {
        return tapPerMinute;
    }

    public void setTapPerMinute(int tapPerMinute) {
        this.tapPerMinute = tapPerMinute;
    }

    public int getTelemetryPerMinute() {
        return telemetryPerMinute;
    }

    public void setTelemetryPerMinute(int telemetryPerMinute) {
        this.telemetryPerMinute = telemetryPerMinute;
    }
}

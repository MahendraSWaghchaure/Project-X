package com.projectx.tracking.ingest;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.time.Instant;

/**
 * Shape of Traccar's JSON position-forwarding payload (Settings -> Notifications -> "Forward
 * events"/position forwarding, format=json). The `device` block only appears if Traccar is
 * configured with forward.additional=true - without it we can't correlate a ping to one of our
 * Vehicles, since we key off Vehicle.gpsDeviceId == device.uniqueId (Traccar's own device
 * identifier), not Traccar's internal numeric deviceId.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record TraccarWebhookPayload(TraccarPosition position, TraccarDevice device) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record TraccarPosition(Double latitude, Double longitude, Double speed, Double course, Instant fixTime) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record TraccarDevice(String uniqueId) {
    }
}

package com.projectx.tracking.ingest;

import com.projectx.school.domain.Vehicle;
import com.projectx.school.repo.VehicleRepository;
import com.projectx.tracking.service.LocationIngestService;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Public endpoint (see module-identity's SecurityConfig - /tracking/ingest/** is permitted
 * without a JWT, since the caller is our self-hosted Traccar server, not an app user) protected
 * instead by a path-embedded shared secret. Point this at your Traccar server's position
 * forwarding config: http://&lt;host&gt;/tracking/ingest/traccar/{sharedSecret}.
 */
@RestController
@RequestMapping("/tracking/ingest/traccar")
@EnableConfigurationProperties(TrackingIngestProperties.class)
public class TrackingIngestController {

    private static final Logger log = LoggerFactory.getLogger(TrackingIngestController.class);
    private static final double KNOTS_TO_KMH = 1.852;

    private final TrackingIngestProperties properties;
    private final VehicleRepository vehicleRepository;
    private final LocationIngestService locationIngestService;

    public TrackingIngestController(TrackingIngestProperties properties, VehicleRepository vehicleRepository,
                                     LocationIngestService locationIngestService) {
        this.properties = properties;
        this.vehicleRepository = vehicleRepository;
        this.locationIngestService = locationIngestService;
    }

    @PostMapping("/{token}")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void ingest(@PathVariable String token, @RequestBody TraccarWebhookPayload payload) {
        if (properties.getSharedSecret() == null || !properties.getSharedSecret().equals(token)) {
            log.warn("Rejected Traccar webhook call with invalid token");
            return;
        }
        if (payload.position() == null || payload.device() == null || payload.device().uniqueId() == null) {
            log.warn("Traccar webhook payload missing position/device.uniqueId - is forward.additional=true set?");
            return;
        }

        String gpsDeviceId = payload.device().uniqueId();
        Vehicle vehicle = vehicleRepository.findByGpsDeviceId(gpsDeviceId).orElse(null);
        if (vehicle == null) {
            log.warn("No vehicle registered for Traccar device uniqueId={}", gpsDeviceId);
            return;
        }

        var position = payload.position();
        Double speedKmh = position.speed() != null ? position.speed() * KNOTS_TO_KMH : null;
        Instant recordedAt = position.fixTime() != null ? position.fixTime() : Instant.now();

        locationIngestService.recordPing(vehicle, position.latitude(), position.longitude(), speedKmh, position.course(), recordedAt);
    }
}

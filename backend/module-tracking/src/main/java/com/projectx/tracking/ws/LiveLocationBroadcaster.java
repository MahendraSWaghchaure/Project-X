package com.projectx.tracking.ws;

import com.projectx.tracking.cache.CachedLocation;
import org.springframework.stereotype.Component;

/** Every ping fans out here so the parent/school-admin map updates live, Swiggy-rider-style. Goes through Redis pub/sub (StompRelayPublisher) so it reaches every app instance, not just whichever one ingested this ping. */
@Component
public class LiveLocationBroadcaster {

    private final StompRelayPublisher relayPublisher;

    public LiveLocationBroadcaster(StompRelayPublisher relayPublisher) {
        this.relayPublisher = relayPublisher;
    }

    public void broadcast(CachedLocation location) {
        relayPublisher.publish("/topic/vehicles/" + location.vehicleId() + "/location", location);
        if (location.tripId() != null) {
            relayPublisher.publish("/topic/trips/" + location.tripId() + "/location", location);
        }
    }
}

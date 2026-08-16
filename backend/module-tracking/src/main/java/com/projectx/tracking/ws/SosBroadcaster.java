package com.projectx.tracking.ws;

import com.projectx.tracking.dto.SosEventResponse;
import org.springframework.stereotype.Component;

/** Pushes an SOS trigger/resolve to anyone watching this trip live - parent map screen, school-admin dashboard. Goes through the same Redis relay as location updates, for the same multi-instance reason. */
@Component
public class SosBroadcaster {

    private final StompRelayPublisher relayPublisher;

    public SosBroadcaster(StompRelayPublisher relayPublisher) {
        this.relayPublisher = relayPublisher;
    }

    public void broadcast(SosEventResponse event) {
        relayPublisher.publish("/topic/trips/" + event.tripId() + "/sos", event);
    }
}

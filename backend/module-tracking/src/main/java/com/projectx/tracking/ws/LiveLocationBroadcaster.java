package com.projectx.tracking.ws;

import com.projectx.tracking.cache.CachedLocation;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

/** Every ping fans out here so the parent/school-admin map updates live, Swiggy-rider-style. */
@Component
public class LiveLocationBroadcaster {

    private final SimpMessagingTemplate messagingTemplate;

    public LiveLocationBroadcaster(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    public void broadcast(CachedLocation location) {
        messagingTemplate.convertAndSend("/topic/vehicles/" + location.vehicleId() + "/location", location);
        if (location.tripId() != null) {
            messagingTemplate.convertAndSend("/topic/trips/" + location.tripId() + "/location", location);
        }
    }
}

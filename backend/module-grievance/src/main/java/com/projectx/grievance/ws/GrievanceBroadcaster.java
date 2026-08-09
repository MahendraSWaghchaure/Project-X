package com.projectx.grievance.ws;

import com.projectx.grievance.dto.GrievanceResponse;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

/**
 * Fans a grievance out to whoever should see it update live, over the same STOMP broker
 * module-tracking already registers app-wide (see TrackingWebSocketConfig) - no second broker
 * needed, just another set of /topic destinations.
 */
@Component
public class GrievanceBroadcaster {

    private final SimpMessagingTemplate messagingTemplate;

    public GrievanceBroadcaster(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    public void broadcast(GrievanceResponse grievance) {
        messagingTemplate.convertAndSend("/topic/users/" + grievance.raisedByUserId() + "/grievances", grievance);
        if (grievance.schoolId() != null) {
            messagingTemplate.convertAndSend("/topic/schools/" + grievance.schoolId() + "/grievances", grievance);
        }
        messagingTemplate.convertAndSend("/topic/platform-admin/grievances", grievance);
    }
}

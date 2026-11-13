package com.projectx.notification.sender;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** Mirrors TwilioProperties' shape: disabled by default, two properties to turn it on. */
@ConfigurationProperties(prefix = "projectx.notification.push")
public class FirebaseProperties {

    private boolean enabled = false;
    private String serviceAccountJsonPath;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getServiceAccountJsonPath() {
        return serviceAccountJsonPath;
    }

    public void setServiceAccountJsonPath(String serviceAccountJsonPath) {
        this.serviceAccountJsonPath = serviceAccountJsonPath;
    }
}

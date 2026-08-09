package com.projectx.tracking.ingest;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "projectx.tracking.ingest")
public class TrackingIngestProperties {

    /** Shared secret embedded in the webhook URL Traccar is configured to POST to; rejects anyone who doesn't know it. */
    private String sharedSecret;

    public String getSharedSecret() {
        return sharedSecret;
    }

    public void setSharedSecret(String sharedSecret) {
        this.sharedSecret = sharedSecret;
    }
}

package com.projectx.people.service;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "projectx.people.qr")
public class ChildQrProperties {

    /** Signing key for child boarding-QR tokens. See ChildQrTokenService for the ephemeral-default behavior if unset. */
    private String secret;

    public String getSecret() {
        return secret;
    }

    public void setSecret(String secret) {
        this.secret = secret;
    }
}

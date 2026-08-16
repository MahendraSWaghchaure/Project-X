package com.projectx.people.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.UUID;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Service;

/**
 * Generates and verifies the stable per-child QR token a driver scans to mark boarding, instead
 * of tapping a button. HMAC-signed so a photo of one child's code can't be edited/retyped into a
 * different child's id; not a one-time token like password-reset - the same code is re-scanned
 * every trip, so it's deterministic (no DB round trip to check it), not random-then-hashed.
 */
@Service
@EnableConfigurationProperties(ChildQrProperties.class)
public class ChildQrTokenService {

    private static final Logger log = LoggerFactory.getLogger(ChildQrTokenService.class);

    private final SecretKeySpec key;

    public ChildQrTokenService(ChildQrProperties properties) {
        String secret = properties.getSecret();
        if (secret == null || secret.isBlank()) {
            log.warn("No projectx.people.qr.secret configured - generating an EPHEMERAL secret for this process. "
                    + "Every restart invalidates all previously generated child QR codes, and this must never be "
                    + "used in production or with more than one app instance.");
            secret = UUID.randomUUID().toString();
        }
        this.key = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
    }

    public String generateToken(Long childId) {
        return childId + "." + sign(String.valueOf(childId));
    }

    public Long resolveChildId(String token) {
        int dot = token == null ? -1 : token.indexOf('.');
        if (dot <= 0) {
            throw new IllegalArgumentException("Malformed QR code");
        }
        String childIdPart = token.substring(0, dot);
        String signature = token.substring(dot + 1);
        String expected = sign(childIdPart);
        if (!MessageDigest.isEqual(expected.getBytes(StandardCharsets.UTF_8), signature.getBytes(StandardCharsets.UTF_8))) {
            throw new IllegalArgumentException("Invalid or tampered QR code");
        }
        try {
            return Long.parseLong(childIdPart);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Malformed QR code");
        }
    }

    private String sign(String value) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(key);
            byte[] result = mac.doFinal(value.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(result);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}

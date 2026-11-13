package com.projectx.hardware.security;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * Same discipline as every other secret-token hash in this codebase (module-attendance's consent
 * tokens, module-identity's refresh tokens): only this hash is ever persisted, never the raw key.
 * Plain SHA-256 (not bcrypt) is deliberate here - the raw key is a high-entropy random value
 * generated for the device, not a human-chosen password, so there is nothing for a slow KDF to
 * protect against beyond what a fast, constant-time-compared hash already gives.
 */
public final class DeviceKeyHasher {

    private DeviceKeyHasher() {
    }

    public static String sha256Hex(String rawKey) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(rawKey.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }

    public static boolean matches(String rawKey, String storedHash) {
        String candidate = sha256Hex(rawKey);
        return MessageDigest.isEqual(candidate.getBytes(StandardCharsets.UTF_8), storedHash.getBytes(StandardCharsets.UTF_8));
    }
}

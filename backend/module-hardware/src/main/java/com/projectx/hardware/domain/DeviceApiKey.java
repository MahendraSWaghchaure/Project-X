package com.projectx.hardware.domain;

import com.projectx.common.domain.Auditable;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * A per-device API key for the hardware ingestion endpoints, checked by
 * {@code DeviceApiKeyAuthInterceptor} - deliberately separate from module-identity's JWT stack
 * (see HardwareIngestController's javadoc for why: a tap/telemetry device has no user login).
 * Only the SHA-256 hex of the raw key is ever stored, same discipline module-attendance's
 * consent-link tokens follow - the raw key is handed to whoever provisions the device once and
 * never persisted or logged anywhere.
 *
 * <p>There is deliberately no admin endpoint to create/rotate these yet (out of scope - see the
 * module javadoc on real hardware not existing yet); a platform-admin-only CRUD surface is the
 * obvious next step once a real device needs onboarding. For now a row is inserted directly
 * (migration-seeded or hand-inserted) when a device is provisioned.
 */
@Entity
@Table(name = "device_api_keys")
public class DeviceApiKey extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "device_id", nullable = false, unique = true, length = 100)
    private String deviceId;

    @Column(name = "key_hash", nullable = false, length = 64)
    private String keyHash;

    @Column(length = 200)
    private String label;

    @Column(nullable = false)
    private boolean active = true;

    protected DeviceApiKey() {
    }

    public DeviceApiKey(String deviceId, String keyHash, String label) {
        this.deviceId = deviceId;
        this.keyHash = keyHash;
        this.label = label;
    }

    public Long getId() {
        return id;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public String getKeyHash() {
        return keyHash;
    }

    public String getLabel() {
        return label;
    }

    public boolean isActive() {
        return active;
    }

    public void deactivate() {
        this.active = false;
    }
}

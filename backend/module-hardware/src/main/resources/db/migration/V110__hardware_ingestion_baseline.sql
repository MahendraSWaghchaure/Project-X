-- module-hardware owns Flyway version range V110-V119 (next free range after module-attendance's
-- V100-V109 - see ARCHITECTURE.md for the full module/range map).
--
-- Raw ingestion storage for hardware that does not exist yet in this deployment (see
-- HardwareIngestController's javadoc) - a per-device API key table (device_api_keys), and two
-- idempotent sample tables (rfid_tap_events, vehicle_telemetry_samples). Nothing here is read by
-- any other module.

CREATE TABLE device_api_keys (
    id BIGSERIAL PRIMARY KEY,
    device_id VARCHAR(100) NOT NULL UNIQUE,
    key_hash VARCHAR(64) NOT NULL,
    label VARCHAR(200),
    active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE rfid_tap_events (
    id BIGSERIAL PRIMARY KEY,
    device_id VARCHAR(100) NOT NULL,
    card_token VARCHAR(200) NOT NULL,
    gate_type VARCHAR(16) NOT NULL,
    event_timestamp TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- The idempotency guarantee itself (HardwareIngestService's exists-check is only the fast path) -
-- a retried tap from flaky hardware with the same (device_id, event_timestamp) cannot double-write.
CREATE UNIQUE INDEX uq_rfid_tap_events_device_timestamp ON rfid_tap_events (device_id, event_timestamp);

CREATE TABLE vehicle_telemetry_samples (
    id BIGSERIAL PRIMARY KEY,
    device_id VARCHAR(100) NOT NULL,
    speed_kmh DOUBLE PRECISION,
    harsh_braking BOOLEAN NOT NULL DEFAULT false,
    door_open BOOLEAN NOT NULL DEFAULT false,
    sample_timestamp TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE UNIQUE INDEX uq_vehicle_telemetry_samples_device_timestamp ON vehicle_telemetry_samples (device_id, sample_timestamp);

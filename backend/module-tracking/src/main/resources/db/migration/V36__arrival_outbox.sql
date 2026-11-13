-- Transactional outbox for the arrival-opening pipeline (production hardening, 2026-11-13).
--
-- ChildTripEventService inserts one row here in the SAME transaction as the child_trip_events row
-- it mirrors - so "this event needs an Arrival opened" is durable the instant the event itself is
-- durable, with no window for loss between the two writes. module-attendance's ArrivalReconciler
-- polls unprocessed rows (processed_at IS NULL) and marks them processed in the same transaction it
-- creates/advances the Arrival (see ArrivalService.processOutboxEvent). The AFTER_COMMIT listener
-- (ArrivalEventListeners.onDriverEvent) remains the fast path (~100ms); this table is what
-- guarantees eventual correctness even if that listener never runs (crash, executor rejection, etc).
--
-- No FK to trips/children (cross-module id references, like the rest of this schema); trip_event_id
-- does reference child_trip_events since both tables live in this same module.
CREATE TABLE arrival_outbox (
    id BIGSERIAL PRIMARY KEY,
    trip_event_id BIGINT NOT NULL REFERENCES child_trip_events (id),
    trip_id BIGINT NOT NULL,
    child_id BIGINT NOT NULL,
    school_id BIGINT NOT NULL,
    event_type VARCHAR(24) NOT NULL,
    confirmed_by_user_id BIGINT NOT NULL,
    occurred_at TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    processed_at TIMESTAMPTZ,
    CONSTRAINT uq_arrival_outbox_trip_event UNIQUE (trip_event_id)
);

-- The poller only ever looks at unprocessed rows; a partial index keeps that scan tiny forever
-- regardless of how large the table grows (most rows are processed within one poll interval).
CREATE INDEX idx_arrival_outbox_unprocessed ON arrival_outbox (id) WHERE processed_at IS NULL;

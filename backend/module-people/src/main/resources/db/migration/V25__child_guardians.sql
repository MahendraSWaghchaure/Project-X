-- Unlimited guardians per child: children.secondary_parent_id (a single nullable FK) only ever
-- allowed exactly one guardian beyond the primary. This table holds every guardian BEYOND the
-- primary (children.primary_parent_id stays as-is - it remains the one guardian the routing
-- auto-allocation and fee-invoice billing anchor use, unchanged from today).
CREATE TABLE child_guardians (
    id BIGSERIAL PRIMARY KEY,
    child_id BIGINT NOT NULL REFERENCES children (id) ON DELETE CASCADE,
    parent_id BIGINT NOT NULL REFERENCES parent_profiles (id) ON DELETE CASCADE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_child_guardians_child_parent UNIQUE (child_id, parent_id)
);

CREATE INDEX idx_child_guardians_child_id ON child_guardians (child_id);
CREATE INDEX idx_child_guardians_parent_id ON child_guardians (parent_id);

-- Backfill: every existing secondary guardian becomes a child_guardians row.
INSERT INTO child_guardians (child_id, parent_id)
SELECT id, secondary_parent_id FROM children WHERE secondary_parent_id IS NOT NULL;

ALTER TABLE children DROP COLUMN secondary_parent_id;

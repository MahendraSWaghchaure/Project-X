CREATE TABLE child_absences (
    id BIGSERIAL PRIMARY KEY,
    child_id BIGINT NOT NULL REFERENCES children (id) ON DELETE CASCADE,
    absence_date DATE NOT NULL,
    marked_by_user_id BIGINT NOT NULL REFERENCES users (id),
    UNIQUE (child_id, absence_date)
);

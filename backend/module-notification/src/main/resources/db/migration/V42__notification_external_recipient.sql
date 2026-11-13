-- module-notification owns Flyway version range V40-V49.
-- Some messages go to a phone number that is not a user account (e.g. a guardian's consent-link SMS), so the recipient
-- user becomes optional and the (masked) contact is recorded instead.
ALTER TABLE notification_records ALTER COLUMN recipient_user_id DROP NOT NULL;
ALTER TABLE notification_records ADD COLUMN recipient_contact VARCHAR(64);

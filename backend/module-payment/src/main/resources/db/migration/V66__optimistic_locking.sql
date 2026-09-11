-- Backs @Version on Payment, FeeInvoice, and DriverBankDetail - guards concurrent status
-- transitions/edits on the highest-stakes entities in this module from silently losing a write.
ALTER TABLE payments ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
ALTER TABLE fee_invoices ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
ALTER TABLE driver_bank_details ADD COLUMN version BIGINT NOT NULL DEFAULT 0;

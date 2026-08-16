-- Belt-and-suspenders alongside the service-layer pre-check (see DriverBankDetailService) - the
-- same TOCTOU race the email uniqueness check has (two concurrent requests both passing the
-- pre-check before either commits) applies here too, and this is what catches it.
ALTER TABLE driver_bank_details ADD CONSTRAINT uq_driver_bank_details_account_number UNIQUE (account_number);

-- Distinct from transaction_reference (which prefers the bank UTR once assigned - see
-- RazorpayPayoutClient.createPayout): PaymentReconciliationService has to fetch-by-id against
-- Razorpay's payouts API, and a UTR is not a valid payout id to fetch with.
ALTER TABLE payroll_records ADD COLUMN razorpay_payout_id VARCHAR(64);

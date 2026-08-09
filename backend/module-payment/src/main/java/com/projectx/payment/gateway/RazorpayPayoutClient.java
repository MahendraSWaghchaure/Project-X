package com.projectx.payment.gateway;

import java.math.BigDecimal;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * RazorpayX Payouts (https://razorpay.com/docs/x/payout-links/api/) - a driver's bank details go
 * through a three-step setup (Contact -> Fund Account -> Payout) the first time they're paid,
 * then just the Payout call on every subsequent run since DriverBankDetail caches the contact/
 * fund-account ids. Requires a real, KYC'd, funded RazorpayX account - see RazorpayProperties.
 */
@Component
public class RazorpayPayoutClient {

    private final RazorpayHttpClient httpClient;
    private final RazorpayProperties properties;

    public RazorpayPayoutClient(RazorpayHttpClient httpClient, RazorpayProperties properties) {
        this.httpClient = httpClient;
        this.properties = properties;
    }

    public boolean isEnabled() {
        return properties.isPayoutsEnabled();
    }

    private void requireEnabled() {
        if (!properties.isPayoutsEnabled()) {
            throw new PaymentGatewayException(
                    "RazorpayX payouts are not configured (set projectx.payment.razorpay.payouts-enabled=true "
                            + "with a real, KYC'd RazorpayX account before running driver payroll disbursement)");
        }
    }

    public String createContact(String name, String reference) {
        requireEnabled();
        Map<String, Object> body = Map.of(
                "name", name,
                "type", "employee",
                "reference_id", reference);
        Map<String, Object> response = httpClient.post("/contacts", body);
        return (String) response.get("id");
    }

    public String createFundAccount(String contactId, String accountHolderName, String accountNumber, String ifscCode) {
        requireEnabled();
        Map<String, Object> body = Map.of(
                "contact_id", contactId,
                "account_type", "bank_account",
                "bank_account", Map.of(
                        "name", accountHolderName,
                        "ifsc", ifscCode,
                        "account_number", accountNumber));
        Map<String, Object> response = httpClient.post("/fund_accounts", body);
        return (String) response.get("id");
    }

    public PayoutResult createPayout(String fundAccountId, BigDecimal amountInRupees, String referenceId, String narration) {
        requireEnabled();
        long amountInPaise = amountInRupees.movePointRight(2).longValueExact();
        Map<String, Object> body = Map.of(
                "account_number", properties.getPayoutAccountNumber(),
                "fund_account_id", fundAccountId,
                "amount", amountInPaise,
                "currency", "INR",
                "mode", "IMPS",
                "purpose", "salary",
                "queue_if_low_balance", true,
                "reference_id", referenceId,
                "narration", narration);
        Map<String, Object> response = httpClient.post("/payouts", body);
        String payoutId = (String) response.get("id");
        Object utr = response.get("utr");
        String status = (String) response.get("status");
        // utr (the actual bank transaction number) is only assigned once the bank processes it -
        // fall back to the Razorpay payout id as the reference until then, still a real, traceable id.
        String reference = utr != null ? utr.toString() : payoutId;
        return new PayoutResult(payoutId, reference, status);
    }

    public record PayoutResult(String payoutId, String transactionReference, String status) {
    }
}

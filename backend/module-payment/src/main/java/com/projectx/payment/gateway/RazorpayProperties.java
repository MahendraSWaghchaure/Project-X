package com.projectx.payment.gateway;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Both halves default OFF, same pattern as Twilio/SMTP elsewhere in this codebase - a fresh dev
 * environment must be able to boot and run without real payment credentials. Orders/Checkout
 * (parent fee collection) and Payouts (driver salary disbursement) are independent toggles since
 * they need entirely different Razorpay product setup (a plain Razorpay account vs a KYC'd
 * RazorpayX account with real balance).
 */
@ConfigurationProperties(prefix = "projectx.payment.razorpay")
public class RazorpayProperties {

    /** Enables Orders API + checkout signature verification (parent payments). */
    private boolean enabled = false;
    private String keyId = "";
    private String keySecret = "";

    /** Enables the RazorpayX Payouts flow (driver salary disbursement) - needs a real, funded RazorpayX account. */
    private boolean payoutsEnabled = false;

    /** The RazorpayX virtual account payouts are drawn from - found on the RazorpayX dashboard. */
    private String payoutAccountNumber = "";

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getKeyId() {
        return keyId;
    }

    public void setKeyId(String keyId) {
        this.keyId = keyId;
    }

    public String getKeySecret() {
        return keySecret;
    }

    public void setKeySecret(String keySecret) {
        this.keySecret = keySecret;
    }

    public boolean isPayoutsEnabled() {
        return payoutsEnabled;
    }

    public void setPayoutsEnabled(boolean payoutsEnabled) {
        this.payoutsEnabled = payoutsEnabled;
    }

    public String getPayoutAccountNumber() {
        return payoutAccountNumber;
    }

    public void setPayoutAccountNumber(String payoutAccountNumber) {
        this.payoutAccountNumber = payoutAccountNumber;
    }
}

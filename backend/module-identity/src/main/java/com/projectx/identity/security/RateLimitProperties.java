package com.projectx.identity.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "projectx.security.rate-limit")
public class RateLimitProperties {

    private int loginPerMinute = 20;
    private int refreshPerMinute = 30;
    private int driverRegisterPerMinute = 5;
    private int forgotPasswordPerMinute = 5;
    private int sosTriggerPerMinute = 10;
    private int childCreatePerMinute = 20;
    private int paymentOrderPerMinute = 15;
    /** PUT /schools/{id}/attendance/bulk - a teacher submits one sheet per class per period; 60/min per IP leaves room for a whole school behind one NAT. */
    private int attendanceBulkPerMinute = 60;
    /** POST /schools/{id}/students/import - up to 500 rows each, so deliberately tight. */
    private int studentImportPerMinute = 10;
    /** POST /parents/me/arrivals/{id}/respond */
    private int arrivalRespondPerMinute = 60;
    /** Public, unauthenticated: GET /public/consent/{token} (view the consent request). */
    private int consentViewPerMinute = 30;
    /** Public, unauthenticated: POST /public/consent/{token}/respond. */
    private int consentRespondPerMinute = 10;
    /** Public, unauthenticated: POST /public/inquiries (footer contact form). */
    private int inquiryPerMinute = 5;
    /** Authenticated but sends an SMS to a third party: POST /schools/{id}/students/{id}/consent-link. */
    private int consentLinkPerMinute = 20;
    /**
     * Exact IPs allowed to set X-Forwarded-For and have it trusted as the real client address.
     * Empty by default - on a bare box (no reverse proxy in front), the header is attacker-controlled
     * and must be ignored entirely, or the limiter above is a no-op (a fresh value per request always
     * resets the bucket). In production, set this to the reverse proxy's own address (e.g. nginx on
     * 127.0.0.1, or the load balancer's private IP) via PROJECTX_SECURITY_RATE_LIMIT_TRUSTED_PROXIES
     * (comma-separated). Loopback is included by default since the dev/local profile always runs
     * behind nothing - this must be overridden, not just left as-is, the moment a real proxy is added.
     */
    private java.util.List<String> trustedProxies = java.util.List.of("127.0.0.1", "0:0:0:0:0:0:0:1", "::1");

    public java.util.List<String> getTrustedProxies() {
        return trustedProxies;
    }

    public void setTrustedProxies(java.util.List<String> trustedProxies) {
        this.trustedProxies = trustedProxies;
    }

    public int getConsentViewPerMinute() {
        return consentViewPerMinute;
    }

    public void setConsentViewPerMinute(int consentViewPerMinute) {
        this.consentViewPerMinute = consentViewPerMinute;
    }

    public int getConsentRespondPerMinute() {
        return consentRespondPerMinute;
    }

    public void setConsentRespondPerMinute(int consentRespondPerMinute) {
        this.consentRespondPerMinute = consentRespondPerMinute;
    }

    public int getInquiryPerMinute() {
        return inquiryPerMinute;
    }

    public void setInquiryPerMinute(int inquiryPerMinute) {
        this.inquiryPerMinute = inquiryPerMinute;
    }

    public int getConsentLinkPerMinute() {
        return consentLinkPerMinute;
    }

    public void setConsentLinkPerMinute(int consentLinkPerMinute) {
        this.consentLinkPerMinute = consentLinkPerMinute;
    }

    public int getAttendanceBulkPerMinute() {
        return attendanceBulkPerMinute;
    }

    public void setAttendanceBulkPerMinute(int attendanceBulkPerMinute) {
        this.attendanceBulkPerMinute = attendanceBulkPerMinute;
    }

    public int getStudentImportPerMinute() {
        return studentImportPerMinute;
    }

    public void setStudentImportPerMinute(int studentImportPerMinute) {
        this.studentImportPerMinute = studentImportPerMinute;
    }

    public int getArrivalRespondPerMinute() {
        return arrivalRespondPerMinute;
    }

    public void setArrivalRespondPerMinute(int arrivalRespondPerMinute) {
        this.arrivalRespondPerMinute = arrivalRespondPerMinute;
    }

    public int getLoginPerMinute() {
        return loginPerMinute;
    }

    public void setLoginPerMinute(int loginPerMinute) {
        this.loginPerMinute = loginPerMinute;
    }

    public int getRefreshPerMinute() {
        return refreshPerMinute;
    }

    public void setRefreshPerMinute(int refreshPerMinute) {
        this.refreshPerMinute = refreshPerMinute;
    }

    public int getDriverRegisterPerMinute() {
        return driverRegisterPerMinute;
    }

    public void setDriverRegisterPerMinute(int driverRegisterPerMinute) {
        this.driverRegisterPerMinute = driverRegisterPerMinute;
    }

    public int getForgotPasswordPerMinute() {
        return forgotPasswordPerMinute;
    }

    public void setForgotPasswordPerMinute(int forgotPasswordPerMinute) {
        this.forgotPasswordPerMinute = forgotPasswordPerMinute;
    }

    public int getSosTriggerPerMinute() {
        return sosTriggerPerMinute;
    }

    public void setSosTriggerPerMinute(int sosTriggerPerMinute) {
        this.sosTriggerPerMinute = sosTriggerPerMinute;
    }

    public int getChildCreatePerMinute() {
        return childCreatePerMinute;
    }

    public void setChildCreatePerMinute(int childCreatePerMinute) {
        this.childCreatePerMinute = childCreatePerMinute;
    }

    public int getPaymentOrderPerMinute() {
        return paymentOrderPerMinute;
    }

    public void setPaymentOrderPerMinute(int paymentOrderPerMinute) {
        this.paymentOrderPerMinute = paymentOrderPerMinute;
    }
}

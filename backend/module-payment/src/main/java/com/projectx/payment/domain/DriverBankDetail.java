package com.projectx.payment.domain;

import com.projectx.common.domain.Auditable;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

/**
 * razorpayContactId/razorpayFundAccountId are cached once RazorpayX creates them for this driver,
 * so PayrollDisbursementService only has to do that setup once per driver instead of on every
 * payout - see RazorpayPayoutClient.
 */
@Entity
@Table(name = "driver_bank_details")
public class DriverBankDetail extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Guards a driver's own concurrent bank-detail edits (e.g. two tabs saving at once) from silently losing one. */
    @Version
    private Long version;

    @Column(name = "driver_id", nullable = false, unique = true)
    private Long driverId;

    @Column(name = "account_holder_name", nullable = false)
    private String accountHolderName;

    @Column(name = "account_number", nullable = false)
    private String accountNumber;

    @Column(name = "ifsc_code", nullable = false)
    private String ifscCode;

    @Column(name = "razorpay_contact_id")
    private String razorpayContactId;

    @Column(name = "razorpay_fund_account_id")
    private String razorpayFundAccountId;

    protected DriverBankDetail() {
    }

    public DriverBankDetail(Long driverId, String accountHolderName, String accountNumber, String ifscCode) {
        this.driverId = driverId;
        this.accountHolderName = accountHolderName;
        this.accountNumber = accountNumber;
        this.ifscCode = ifscCode;
    }

    public Long getId() {
        return id;
    }

    public Long getDriverId() {
        return driverId;
    }

    public String getAccountHolderName() {
        return accountHolderName;
    }

    public String getAccountNumber() {
        return accountNumber;
    }

    public String getIfscCode() {
        return ifscCode;
    }

    public String getRazorpayContactId() {
        return razorpayContactId;
    }

    public String getRazorpayFundAccountId() {
        return razorpayFundAccountId;
    }

    public void updateDetails(String accountHolderName, String accountNumber, String ifscCode) {
        this.accountHolderName = accountHolderName;
        this.accountNumber = accountNumber;
        this.ifscCode = ifscCode;
        // Bank details changed - any cached RazorpayX fund account is now stale and must be recreated.
        this.razorpayContactId = null;
        this.razorpayFundAccountId = null;
    }

    public void cacheRazorpayIds(String contactId, String fundAccountId) {
        this.razorpayContactId = contactId;
        this.razorpayFundAccountId = fundAccountId;
    }
}

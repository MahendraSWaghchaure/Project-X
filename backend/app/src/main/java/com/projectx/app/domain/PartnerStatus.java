package com.projectx.app.domain;

/** Always COMING_SOON for now (F1) - no real onboarding/policy-issuance/payment flow exists yet; kept as an enum (not a
 * boolean) so a later phase can add ACTIVE/INACTIVE without a schema change. */
public enum PartnerStatus {
    COMING_SOON
}

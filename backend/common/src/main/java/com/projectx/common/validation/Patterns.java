package com.projectx.common.validation;

/**
 * Shared regex constants for {@code @Pattern} annotations on request DTOs, so a format rule
 * doesn't drift between the ~9 phone-number fields spread across module-school and module-people.
 */
public final class Patterns {

    /**
     * Permissive international phone check: optional leading {@code +}, 7-15 digits. Not
     * India-only (no fixed length/prefix) since school contact numbers and emergency contacts may
     * be landlines or non-Indian numbers - this is a sanity check against garbage input, not a
     * telecom-grade validator.
     */
    public static final String PHONE = "^\\+?[0-9]{7,15}$";

    public static final String PHONE_MESSAGE = "must be a valid phone number (digits only, optional leading +)";

    private Patterns() {
    }
}

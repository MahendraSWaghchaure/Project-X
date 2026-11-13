package com.projectx.people.domain;

public enum DriverEmploymentStatus {
    ACTIVE,
    ON_LEAVE,
    /** Temporarily stopped by the school; requires a reason, reversible (back to ACTIVE). */
    SUSPENDED,
    TERMINATED
}

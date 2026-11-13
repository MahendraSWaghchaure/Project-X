package com.projectx.common.security;

/**
 * The end-user roles from the product spec, plus the platform's own operating role.
 * PLATFORM_ADMIN is us (the service provider) - onboards schools and has cross-tenant oversight.
 */
public enum Role {
    PLATFORM_ADMIN,
    SCHOOL_ADMIN,
    DRIVER,
    PARENT,
    /** School-scoped teaching staff - marks class attendance, records assessments and comments; no access to transport, billing or admin functions. */
    TEACHER
}

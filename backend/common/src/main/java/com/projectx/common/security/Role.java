package com.projectx.common.security;

/**
 * The four end-user roles from the product spec, plus the platform's own operating role.
 * PLATFORM_ADMIN is us (the service provider) - onboards schools and has cross-tenant oversight.
 */
public enum Role {
    PLATFORM_ADMIN,
    SCHOOL_ADMIN,
    DRIVER,
    PARENT
}

package com.projectx.identity.domain;

public enum UserStatus {
    /** Invited but hasn't completed registration (e.g. a parent who hasn't accepted the invite yet). */
    INVITED,
    ACTIVE,
    DISABLED
}

package com.projectx.common.security;

import com.projectx.common.exception.ForbiddenOperationException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Reads the {@link CurrentUser} that module-identity's JWT filter placed as the Authentication
 * principal. Any module can call this without depending on module-identity.
 */
public final class CurrentUserContext {

    private CurrentUserContext() {}

    public static CurrentUser get() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof CurrentUser currentUser)) {
            throw new ForbiddenOperationException("No authenticated user in context");
        }
        return currentUser;
    }

    public static void requireSchoolAccess(Long schoolId) {
        if (!get().belongsToSchool(schoolId)) {
            throw new ForbiddenOperationException("Not authorized for this school");
        }
    }
}

package com.projectx.common.security;

/**
 * The authenticated principal, decoded from the JWT by module-identity and set as the
 * Authentication principal so every other module can read it without depending on identity.
 *
 * @param schoolId tenant scope for SCHOOL_ADMIN/DRIVER; null for PLATFORM_ADMIN (sees all
 *                 schools) and PARENT (scoped by parent/child ownership instead of a school).
 */
public record CurrentUser(Long userId, String email, Role role, Long schoolId) {

    public boolean isPlatformAdmin() {
        return role == Role.PLATFORM_ADMIN;
    }

    public boolean belongsToSchool(Long otherSchoolId) {
        return isPlatformAdmin() || (schoolId != null && schoolId.equals(otherSchoolId));
    }
}

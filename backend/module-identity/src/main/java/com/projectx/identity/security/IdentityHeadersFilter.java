package com.projectx.identity.security;

import com.projectx.common.security.CurrentUser;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Stamps every authenticated response with who/what it was served for - X-User-Id, X-Tenant-Id,
 * X-Workspace-Id - the same identity-correlation idea as TraceIdFilter's X-Request-Id, one level
 * up: that header answers "which request," these answer "on whose behalf."
 *
 * <p>Registered with {@code addFilterAfter(..., BearerTokenAuthenticationFilter.class)} (see
 * SecurityConfig) so it runs strictly after Spring Security's own OAuth2/JWT filter has populated
 * SecurityContextHolder - reading CurrentUserContext any earlier in the chain throws, since there's
 * nothing to read yet. Reads SecurityContextHolder directly rather than CurrentUserContext.get() so
 * a public/unauthenticated endpoint (login, health check) simply gets no headers instead of a
 * ForbiddenOperationException.
 *
 * <p><b>X-Tenant-Id vs X-Workspace-Id:</b> this app has exactly one multi-tenancy boundary today -
 * the school a user belongs to - so both headers currently carry the same value (the school id, or
 * the literal "platform" for a platform admin, who by design belongs to every school rather than
 * one). They're kept as two distinct headers because "tenant" and "workspace" are two different
 * axes in a lot of systems (e.g. one tenant, several workspaces within it) even though this one
 * doesn't have that distinction yet - if it ever does, only this filter needs to change.
 */
public class IdentityHeadersFilter extends OncePerRequestFilter {

    static final String USER_ID_HEADER = "X-User-Id";
    static final String TENANT_ID_HEADER = "X-Tenant-Id";
    static final String WORKSPACE_ID_HEADER = "X-Workspace-Id";
    private static final String PLATFORM_TENANT = "platform";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof CurrentUser currentUser) {
            response.setHeader(USER_ID_HEADER, String.valueOf(currentUser.userId()));
            String tenantId = currentUser.schoolId() != null ? String.valueOf(currentUser.schoolId()) : PLATFORM_TENANT;
            response.setHeader(TENANT_ID_HEADER, tenantId);
            response.setHeader(WORKSPACE_ID_HEADER, tenantId);
        }
        chain.doFilter(request, response);
    }
}

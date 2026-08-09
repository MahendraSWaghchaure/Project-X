package com.projectx.identity.security;

import com.projectx.common.security.CurrentUser;
import com.projectx.common.security.Role;
import java.util.List;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

/**
 * Turns a validated Jwt into a CurrentUser principal (readable by every module via
 * CurrentUserContext) with a single ROLE_xxx authority driving @PreAuthorize checks.
 */
@Component
public class ProjectXJwtAuthenticationConverter implements Converter<Jwt, AbstractAuthenticationToken> {

    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        Role role = Role.valueOf(jwt.getClaimAsString("role"));
        Long schoolId = jwt.hasClaim("schoolId") ? ((Number) jwt.getClaim("schoolId")).longValue() : null;
        CurrentUser currentUser = new CurrentUser(
                Long.valueOf(jwt.getSubject()), jwt.getClaimAsString("email"), role, schoolId);

        List<GrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
        return new JwtAuthentication(jwt, currentUser, authorities);
    }

    private static final class JwtAuthentication extends AbstractAuthenticationToken {
        private final Jwt jwt;
        private final CurrentUser principal;

        JwtAuthentication(Jwt jwt, CurrentUser principal, List<GrantedAuthority> authorities) {
            super(authorities);
            this.jwt = jwt;
            this.principal = principal;
            setAuthenticated(true);
        }

        @Override
        public Object getCredentials() {
            return jwt;
        }

        @Override
        public Object getPrincipal() {
            return principal;
        }
    }
}

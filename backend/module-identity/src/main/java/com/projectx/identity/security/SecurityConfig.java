package com.projectx.identity.security;

import com.projectx.common.trace.TraceIds;
import java.util.List;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@EnableConfigurationProperties({CorsProperties.class, RateLimitProperties.class})
public class SecurityConfig {

    private static final String[] PUBLIC_ENDPOINTS = {
            "/auth/login", "/auth/refresh", "/auth/invitations/*/accept",
            "/auth/forgot-password", "/auth/reset-password",
            "/drivers/register",
            // Public, unauthenticated, rate-limited (RateLimitFilter) - see PublicConsentController / PublicInquiryController.
            "/public/consent/*", "/public/consent/*/respond", "/public/inquiries",
            "/actuator/health/**", "/actuator/info",
            "/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html",
            "/ws/**",
            // Protected by its own path-embedded shared secret instead of a JWT - see TrackingIngestController.
            "/tracking/ingest/**",
            // Protected instead by a per-device API key checked in DeviceApiKeyAuthInterceptor
            // (module-hardware) - hardware has no user login, so there's no JWT to issue it.
            "/hardware/**"
    };

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http,
                                            ProjectXJwtAuthenticationConverter jwtAuthenticationConverter,
                                            RestAuthenticationEntryPoint authenticationEntryPoint,
                                            RestAccessDeniedHandler accessDeniedHandler,
                                            CorsConfigurationSource corsConfigurationSource,
                                            RateLimitFilter rateLimitFilter,
                                            IdentityHeadersFilter identityHeadersFilter) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource))
                .csrf(csrf -> csrf.disable())
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                // Baseline hardening headers. HSTS/frame-options/content-type-sniffing protection are
                // meaningless for a pure JSON API talking to a SPA on another origin, but actuator,
                // swagger-ui, and error pages all still serve HTML from this same app, so they're
                // real attack surface, not theater.
                .headers(headers -> headers
                        .contentTypeOptions(c -> {})
                        .frameOptions(frame -> frame.deny())
                        .httpStrictTransportSecurity(hsts -> hsts.includeSubDomains(true).maxAgeInSeconds(31536000))
                        .referrerPolicy(referrer -> referrer.policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN))
                        // This API never returns HTML except swagger-ui/actuator/the default error page,
                        // but those ARE real attack surface (see the comment above). default-src 'self'
                        // (not 'none' - swagger-ui needs to load its own same-origin bundled JS/CSS) still
                        // blocks any third-party script/style injection, framing, and plugin content.
                        .contentSecurityPolicy(csp -> csp.policyDirectives(
                                "default-src 'self'; frame-ancestors 'none'; base-uri 'self'; object-src 'none'")))
                .addFilterAfter(rateLimitFilter, CorsFilter.class)
                // Must run AFTER BearerTokenAuthenticationFilter (the actual filter class backing
                // oauth2ResourceServer below) - it reads SecurityContextHolder, which that filter is
                // what populates in the first place. Any earlier and there's nothing to read yet.
                .addFilterAfter(identityHeadersFilter, BearerTokenAuthenticationFilter.class)
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(PUBLIC_ENDPOINTS).permitAll()
                        .anyRequest().authenticated())
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter)))
                .exceptionHandling(handling -> handling
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler));
        return http.build();
    }

    @Bean
    public RateLimitFilter rateLimitFilter(StringRedisTemplate redisTemplate, RateLimitProperties rateLimitProperties) {
        return new RateLimitFilter(redisTemplate, rateLimitProperties);
    }

    @Bean
    public IdentityHeadersFilter identityHeadersFilter() {
        return new IdentityHeadersFilter();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource(CorsProperties corsProperties) {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(corsProperties.getAllowedOrigins());
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("Authorization", "Content-Type"));
        // Without this, a browser can't read these response headers via fetch/axios at all in
        // production (a real cross-origin frontend/backend split) - the CORS spec only exposes a
        // small fixed "simple response header" allowlist to page JS by default, and none of these
        // are on it. Dev doesn't hit this (Vite's proxy makes requests same-origin), which is
        // exactly the kind of gap that only shows up after deploying.
        configuration.setExposedHeaders(List.of(
                TraceIds.HEADER, IdentityHeadersFilter.USER_ID_HEADER,
                IdentityHeadersFilter.TENANT_ID_HEADER, IdentityHeadersFilter.WORKSPACE_ID_HEADER));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        // Explicit 12 rather than BCrypt's implicit default of 10 - roughly 4x the hashing cost,
        // a reasonable modern floor without being slow enough to hurt login latency.
        return new BCryptPasswordEncoder(12);
    }
}

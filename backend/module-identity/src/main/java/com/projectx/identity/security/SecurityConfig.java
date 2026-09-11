package com.projectx.identity.security;

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
            "/actuator/health/**", "/actuator/info",
            "/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html",
            "/ws/**",
            // Protected by its own path-embedded shared secret instead of a JWT - see TrackingIngestController.
            "/tracking/ingest/**"
    };

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http,
                                            ProjectXJwtAuthenticationConverter jwtAuthenticationConverter,
                                            RestAuthenticationEntryPoint authenticationEntryPoint,
                                            RestAccessDeniedHandler accessDeniedHandler,
                                            CorsConfigurationSource corsConfigurationSource,
                                            RateLimitFilter rateLimitFilter) throws Exception {
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
    public CorsConfigurationSource corsConfigurationSource(CorsProperties corsProperties) {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(corsProperties.getAllowedOrigins());
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("Authorization", "Content-Type"));
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

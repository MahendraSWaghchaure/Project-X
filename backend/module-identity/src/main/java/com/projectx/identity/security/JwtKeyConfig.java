package com.projectx.identity.security;

import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ResourceLoader;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

/**
 * Wires the RSA keypair used to self-sign access tokens. Point
 * projectx.security.jwt.private/public-key-location at real PEM files (see PemKeyLoader) in any
 * environment other than local dev or the test suite. If left unset there, an ephemeral keypair is
 * generated at startup - fine for those two cases (every restart invalidates outstanding tokens,
 * and it can never be used with more than one app instance), but anywhere else this now REFUSES TO
 * START rather than silently running on a throwaway key - `SPRING_PROFILES_ACTIVE=prod` already
 * gets set correctly by docker-compose.prod.yml, but nothing previously caught a deployment that
 * bypassed that path and forgot to set it. Run locally with `-Dspring-boot.run.profiles=local` or
 * `SPRING_PROFILES_ACTIVE=local` (see README.md) to opt into the ephemeral-key allowance.
 */
@Configuration
@EnableConfigurationProperties(JwtProperties.class)
public class JwtKeyConfig {

    private static final Logger log = LoggerFactory.getLogger(JwtKeyConfig.class);
    private static final String[] EPHEMERAL_KEY_ALLOWED_PROFILES = {"local", "test"};

    @Bean
    public RSAKey rsaJwk(JwtProperties properties, ResourceLoader resourceLoader, Environment environment) {
        KeyPair keyPair = resolveKeyPair(properties, resourceLoader, environment);
        return new RSAKey.Builder((RSAPublicKey) keyPair.getPublic())
                .privateKey((RSAPrivateKey) keyPair.getPrivate())
                .keyID(UUID.randomUUID().toString())
                .build();
    }

    @Bean
    public JwtEncoder jwtEncoder(RSAKey rsaJwk) {
        return new NimbusJwtEncoder(new ImmutableJWKSet<>(new com.nimbusds.jose.jwk.JWKSet(rsaJwk)));
    }

    @Bean
    public JwtDecoder jwtDecoder(RSAKey rsaJwk) throws com.nimbusds.jose.JOSEException {
        return NimbusJwtDecoder.withPublicKey(rsaJwk.toRSAPublicKey()).build();
    }

    private KeyPair resolveKeyPair(JwtProperties properties, ResourceLoader resourceLoader, Environment environment) {
        if (hasText(properties.getPrivateKeyLocation()) && hasText(properties.getPublicKeyLocation())) {
            return PemKeyLoader.load(resourceLoader, properties.getPrivateKeyLocation(), properties.getPublicKeyLocation());
        }
        if (!environment.matchesProfiles(EPHEMERAL_KEY_ALLOWED_PROFILES)) {
            throw new IllegalStateException(
                    "No projectx.security.jwt.private/public-key-location configured, and the active "
                            + "Spring profile(s) " + java.util.Arrays.toString(environment.getActiveProfiles())
                            + " don't include 'local' or 'test'. Refusing to start on an ephemeral JWT key - "
                            + "either configure real PEM key paths, or run with the 'local' profile if this "
                            + "really is local development.");
        }
        log.warn("No projectx.security.jwt.private/public-key-location configured - generating an EPHEMERAL "
                + "RSA keypair for this process. Every restart invalidates all outstanding tokens, and this "
                + "must never be used in production or with more than one app instance.");
        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(2048);
            return generator.generateKeyPair();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    /** application.yml's ${VAR:} placeholder syntax resolves an unset env var to "", not null - both need treating as "unset". */
    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}

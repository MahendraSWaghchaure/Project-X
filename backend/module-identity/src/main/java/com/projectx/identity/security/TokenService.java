package com.projectx.identity.security;

import com.projectx.identity.domain.RefreshToken;
import com.projectx.identity.domain.UserAccount;
import com.projectx.identity.repo.RefreshTokenRepository;
import java.time.Instant;
import java.util.Optional;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class TokenService {

    private final JwtEncoder jwtEncoder;
    private final JwtProperties jwtProperties;
    private final RefreshTokenRepository refreshTokenRepository;

    public TokenService(JwtEncoder jwtEncoder, JwtProperties jwtProperties, RefreshTokenRepository refreshTokenRepository) {
        this.jwtEncoder = jwtEncoder;
        this.jwtProperties = jwtProperties;
        this.refreshTokenRepository = refreshTokenRepository;
    }

    public record IssuedTokens(String accessToken, String refreshToken, long expiresInSeconds) {}

    public IssuedTokens issueTokens(UserAccount user) {
        String accessToken = issueAccessToken(user);
        String refreshToken = issueRefreshToken(user);
        return new IssuedTokens(accessToken, refreshToken, jwtProperties.getAccessTokenTtl().toSeconds());
    }

    public String issueAccessToken(UserAccount user) {
        Instant now = Instant.now();
        JwtClaimsSet.Builder claims = JwtClaimsSet.builder()
                .issuer("projectx")
                .issuedAt(now)
                .expiresAt(now.plus(jwtProperties.getAccessTokenTtl()))
                .subject(user.getId().toString())
                .claim("email", user.getEmail())
                .claim("role", user.getRole().name());
        if (user.getSchoolId() != null) {
            claims.claim("schoolId", user.getSchoolId());
        }
        return jwtEncoder.encode(JwtEncoderParameters.from(JwsHeader.with(SignatureAlgorithm.RS256).build(), claims.build()))
                .getTokenValue();
    }

    private String issueRefreshToken(UserAccount user) {
        String rawToken = TokenHasher.newOpaqueToken();
        RefreshToken entity = new RefreshToken(user.getId(), TokenHasher.sha256(rawToken),
                Instant.now().plus(jwtProperties.getRefreshTokenTtl()));
        refreshTokenRepository.save(entity);
        return rawToken;
    }

    /** Validates + revokes the presented refresh token, returning the user id it belonged to. Rotation happens by the caller issuing a fresh pair. */
    public Optional<Long> consumeRefreshToken(String rawToken) {
        return refreshTokenRepository.findByTokenHash(TokenHasher.sha256(rawToken))
                .filter(RefreshToken::isValid)
                .map(token -> {
                    token.revoke();
                    refreshTokenRepository.save(token);
                    return token.getUserId();
                });
    }

    public void revokeAllForUser(Long userId) {
        refreshTokenRepository.findByUserIdAndRevokedFalse(userId)
                .forEach(RefreshToken::revoke);
    }
}

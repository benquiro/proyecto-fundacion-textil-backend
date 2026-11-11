package cl.textilcircular.api.core.security;

import cl.textilcircular.api.core.model.account.RefreshToken;
import cl.textilcircular.api.core.model.account.UserAccount;
import cl.textilcircular.api.core.repository.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import java.time.Instant;

/**
 * Issues the token pair of a session. The access token names the refresh token it came from
 * ({@code sid}), so "revoke every other session" can keep the caller's own.
 */
@Service
@RequiredArgsConstructor
public class TokenService {

    public static final String ROLE_CLAIM = "role";
    public static final String SESSION_CLAIM = "sid";

    private final JwtEncoder jwtEncoder;
    private final RefreshTokenRepository refreshTokens;
    private final SecurityProperties properties;

    public record TokenPair(String accessToken, String refreshToken, long expiresIn) {
    }

    /** Must run inside the caller's transaction: the refresh token is saved with it. */
    public TokenPair issue(UserAccount account, Instant now) {
        RefreshToken.Issued refresh = RefreshToken.issue(account, now, properties.refreshTtl());
        RefreshToken saved = refreshTokens.save(refresh.token());
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .subject(account.getId().toString())
                .issuedAt(now)
                .expiresAt(now.plus(properties.accessTtl()))
                .claim(ROLE_CLAIM, account.getRole().name())
                .claim(SESSION_CLAIM, saved.getId())
                .build();
        String access = jwtEncoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims))
                .getTokenValue();
        return new TokenPair(access, refresh.rawToken(), properties.accessTtl().toSeconds());
    }
}

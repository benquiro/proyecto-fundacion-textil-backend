package cl.textilcircular.api.core.security;

import cl.textilcircular.api.core.repository.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2ErrorCodes;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Clock;

/**
 * An access token is only good while the session it came from ({@code sid}) is alive, so logout,
 * rotation, password change, suspension and deactivation take effect at once instead of at expiry.
 */
@RequiredArgsConstructor
class SessionValidator implements OAuth2TokenValidator<Jwt> {

    private static final OAuth2TokenValidatorResult ENDED = OAuth2TokenValidatorResult.failure(
            new OAuth2Error(OAuth2ErrorCodes.INVALID_TOKEN, "The session has ended", null));

    private final RefreshTokenRepository refreshTokens;
    private final Clock clock;

    @Override
    public OAuth2TokenValidatorResult validate(Jwt jwt) {
        long sessionId;
        try {
            sessionId = Caller.of(jwt).sessionId();
        } catch (NumberFormatException e) {
            return ENDED;
        }
        return refreshTokens.isActive(sessionId, clock.instant())
                ? OAuth2TokenValidatorResult.success()
                : ENDED;
    }
}

package cl.textilcircular.api.core.security;

import org.springframework.security.oauth2.jwt.Jwt;

/** Who is calling, read from a verified access token. Every access token names the session it belongs to. */
public record Caller(long userId, long sessionId) {

    /** @throws NumberFormatException when the subject or the session claim is missing or not a number */
    public static Caller of(Jwt jwt) {
        if (!(jwt.getClaims().get(TokenService.SESSION_CLAIM) instanceof Number sid)) {
            throw new NumberFormatException("Missing session claim");
        }
        return new Caller(Long.parseLong(jwt.getSubject()), sid.longValue());
    }
}

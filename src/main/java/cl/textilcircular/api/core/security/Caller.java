package cl.textilcircular.api.core.security;

import org.springframework.security.oauth2.jwt.Jwt;

/** Who is calling, read from a verified access token. */
public record Caller(long userId, Long sessionId) {

    public static Caller of(Jwt jwt) {
        Object sid = jwt.getClaims().get(TokenService.SESSION_CLAIM);
        return new Caller(Long.parseLong(jwt.getSubject()), sid instanceof Number n ? n.longValue() : null);
    }
}

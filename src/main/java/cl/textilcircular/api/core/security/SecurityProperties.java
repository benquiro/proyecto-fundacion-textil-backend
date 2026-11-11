package cl.textilcircular.api.core.security;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.time.Duration;

/**
 * @param jwtSecret  HMAC key for access tokens, at least 32 bytes. When blank a random key is
 *                   generated at startup: tokens then stop working after a restart, which is fine
 *                   for local development and never ships a known secret.
 * @param accessTtl  lifetime of an access token
 * @param refreshTtl lifetime of a refresh token
 */
@ConfigurationProperties("app.security")
public record SecurityProperties(
        String jwtSecret,
        @DefaultValue("PT15M") Duration accessTtl,
        @DefaultValue("P30D") Duration refreshTtl) {
}

package cl.textilcircular.api.core.model.account;

import cl.textilcircular.api.core.crypto.Hashes;
import cl.textilcircular.api.core.persistence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Duration;
import java.time.Instant;

/**
 * Long-lived session credential exchanged for short-lived access tokens. Only the SHA-256 of the
 * token is stored; the raw value is returned once, at issue time.
 */
@Getter
@Entity
@Table(name = "refresh_token")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RefreshToken extends BaseEntity {

    private static final int TOKEN_BYTES = 32;

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "refresh_token_seq")
    @SequenceGenerator(name = "refresh_token_seq", sequenceName = "refresh_token_seq", allocationSize = 50)
    @Column(name = "token_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private UserAccount account;

    @Column(name = "token_hash", nullable = false, length = 64)
    private String tokenHash;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "revoked_at")
    private Instant revokedAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    /** The stored token plus the raw value to hand to the client. */
    public record Issued(String rawToken, RefreshToken token) {
    }

    public static Issued issue(UserAccount account, Instant now, Duration ttl) {
        String raw = Hashes.randomToken(TOKEN_BYTES);
        RefreshToken token = new RefreshToken();
        token.account = account;
        token.tokenHash = hash(raw);
        token.createdAt = now;
        token.expiresAt = now.plus(ttl);
        return new Issued(raw, token);
    }

    public static String hash(String rawToken) {
        return Hashes.sha256Hex(rawToken);
    }

    public boolean isActive(Instant now) {
        return revokedAt == null && now.isBefore(expiresAt);
    }

    public void revoke(Instant now) {
        if (revokedAt == null) {
            revokedAt = now;
        }
    }
}

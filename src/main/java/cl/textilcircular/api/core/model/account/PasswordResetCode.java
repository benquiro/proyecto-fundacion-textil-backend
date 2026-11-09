package cl.textilcircular.api.core.model.account;

import cl.textilcircular.api.core.crypto.Hashes;
import cl.textilcircular.api.core.error.ValidationException;
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
 * Six-digit password recovery code. Email delivery is simulated (the code is logged), so only
 * the hash is persisted. The code locks after {@value #MAX_ATTEMPTS} wrong attempts; callers must
 * commit the failed attempt even though {@link #redeem} throws.
 */
@Getter
@Entity
@Table(name = "password_reset_code")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PasswordResetCode extends BaseEntity {

    public static final int MAX_ATTEMPTS = 5;

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "password_reset_code_seq")
    @SequenceGenerator(name = "password_reset_code_seq", sequenceName = "password_reset_code_seq", allocationSize = 50)
    @Column(name = "code_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private UserAccount account;

    @Column(name = "code_hash", nullable = false, length = 64)
    private String codeHash;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "used_at")
    private Instant usedAt;

    @Column(name = "failed_attempts", nullable = false)
    private int failedAttempts;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    public record Issued(String rawCode, PasswordResetCode code) {
    }

    public static Issued issue(UserAccount account, Instant now, Duration ttl) {
        String raw = Hashes.randomDigits(6);
        PasswordResetCode code = new PasswordResetCode();
        code.account = account;
        code.codeHash = Hashes.sha256Hex(raw);
        code.createdAt = now;
        code.expiresAt = now.plus(ttl);
        return new Issued(raw, code);
    }

    /** Marks the code as used when {@code rawCode} matches; counts the attempt otherwise. */
    public void redeem(String rawCode, Instant now) {
        if (usedAt != null || !now.isBefore(expiresAt) || failedAttempts >= MAX_ATTEMPTS) {
            throw new ValidationException("code", "El código expiró o ya no es válido");
        }
        if (rawCode == null || !Hashes.matches(codeHash, rawCode)) {
            failedAttempts++;
            throw new ValidationException("code", "El código no es correcto");
        }
        usedAt = now;
    }
}

package cl.textilcircular.api.account;

import cl.textilcircular.api.shared.error.ForbiddenException;
import cl.textilcircular.api.shared.error.ValidationException;
import cl.textilcircular.api.shared.persistence.AuditedEntity;
import cl.textilcircular.api.shared.validation.Validators;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.UUID;

/**
 * Login identity and account lifecycle. Entrepreneurs also have an {@link Entrepreneur} profile
 * that shares this id.
 */
@Getter
@Entity
@Table(name = "user_account")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserAccount extends AuditedEntity {

    private static final String DELETED_EMAIL_DOMAIN = "@anonymized.invalid";
    private static final String DISABLED_PASSWORD = "!disabled";

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "user_account_seq")
    @SequenceGenerator(name = "user_account_seq", sequenceName = "user_account_seq", allocationSize = 50)
    @Column(name = "user_id")
    private Long id;

    @Column(nullable = false, length = Validators.EMAIL_MAX)
    private String email;

    @Column(name = "password_hash", nullable = false, length = 100)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Role role;

    @Column(length = Validators.ALIAS_MAX)
    private String alias;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AccountStatus status;

    @Column(name = "consent_at", nullable = false)
    private Instant consentAt;

    @Column(name = "deactivated_at")
    private Instant deactivatedAt;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    @Version
    private long version;

    private UserAccount(Role role, String email, String rawPassword, String alias, PasswordEncoder encoder, Instant now) {
        this.role = role;
        this.email = Validators.email(email);
        this.passwordHash = encoder.encode(Validators.password(rawPassword));
        this.alias = validAlias(role, alias);
        this.status = AccountStatus.ACTIVE;
        this.consentAt = now;
    }

    /** Registration accepts the terms and privacy policy, so consent is recorded at {@code now}. */
    public static UserAccount registerCustomer(String email, String rawPassword, String alias,
                                               PasswordEncoder encoder, Instant now) {
        return new UserAccount(Role.CUSTOMER, email, rawPassword, alias, encoder, now);
    }

    /** Account half of the entrepreneur registration; the caller adds the {@link Entrepreneur} profile. */
    public static UserAccount registerEntrepreneur(String email, String rawPassword, String alias,
                                                   PasswordEncoder encoder, Instant now) {
        return new UserAccount(Role.ENTREPRENEUR, email, rawPassword, alias, encoder, now);
    }

    public static UserAccount registerAdministrator(String email, String rawPassword, String alias,
                                                    PasswordEncoder encoder, Instant now) {
        return new UserAccount(Role.ADMINISTRATOR, email, rawPassword, alias, encoder, now);
    }

    /**
     * Checks the password and the status. A deactivated account comes back to life on login;
     * suspended accounts are refused after the password check, so the status is not revealed
     * to someone who does not know the password.
     */
    public void authenticate(String rawPassword, PasswordEncoder encoder, Instant now) {
        if (status == AccountStatus.DELETED || rawPassword == null || !encoder.matches(rawPassword, passwordHash)) {
            throw new InvalidCredentialsException();
        }
        if (status == AccountStatus.SUSPENDED) {
            throw new AccountNotActiveException(status);
        }
        if (status == AccountStatus.DEACTIVATED) {
            activate();
        }
    }

    /** Guard for any operation that needs a usable account. */
    public void ensureActive() {
        if (status != AccountStatus.ACTIVE) {
            throw new AccountNotActiveException(status);
        }
    }

    public boolean isActive() {
        return status == AccountStatus.ACTIVE;
    }

    public boolean isAdministrator() {
        return role == Role.ADMINISTRATOR;
    }

    public boolean isEntrepreneur() {
        return role == Role.ENTREPRENEUR;
    }

    public boolean isCustomer() {
        return role == Role.CUSTOMER;
    }

    public void updateProfile(String email, String alias) {
        ensureActive();
        this.email = Validators.email(email);
        this.alias = validAlias(role, alias);
    }

    public void changePassword(String currentPassword, String newPassword, PasswordEncoder encoder) {
        ensureActive();
        if (currentPassword == null || !encoder.matches(currentPassword, passwordHash)) {
            throw new ValidationException("currentPassword", "La contraseña actual no es correcta");
        }
        if (currentPassword.equals(newPassword)) {
            throw new ValidationException("newPassword", "La nueva contraseña debe ser distinta a la actual");
        }
        try {
            this.passwordHash = encoder.encode(Validators.password(newPassword));
        } catch (ValidationException e) {
            throw new ValidationException("newPassword", e.getMessage());
        }
    }

    /** Sets a new password without the current one (recovery flow, after a valid reset code). */
    public void resetPassword(String newPassword, PasswordEncoder encoder) {
        if (status == AccountStatus.DELETED) {
            throw AccountErrors.invalidStatus("La cuenta fue eliminada");
        }
        this.passwordHash = encoder.encode(Validators.password(newPassword));
    }

    /** The owner pauses the account; it disappears from the platform until the next login. */
    public void deactivate(Instant now) {
        if (status != AccountStatus.ACTIVE) {
            throw AccountErrors.invalidStatus("Solo puedes desactivar una cuenta activa");
        }
        this.status = AccountStatus.DEACTIVATED;
        this.deactivatedAt = now;
    }

    public void suspendBy(UserAccount administrator) {
        requireOtherActiveAdministrator(administrator);
        if (status != AccountStatus.ACTIVE) {
            throw AccountErrors.invalidStatus("Solo se puede suspender una cuenta activa");
        }
        this.status = AccountStatus.SUSPENDED;
    }

    public void reactivateBy(UserAccount administrator) {
        requireOtherActiveAdministrator(administrator);
        if (status != AccountStatus.SUSPENDED && status != AccountStatus.DEACTIVATED) {
            throw AccountErrors.invalidStatus("La cuenta no está suspendida ni desactivada");
        }
        activate();
    }

    /**
     * The owner closes the account. Personal data is removed from this row and the email is
     * replaced so the address can register again; the entrepreneur profile is anonymized separately.
     */
    public void delete(Instant now) {
        if (status == AccountStatus.DELETED) {
            throw AccountErrors.invalidStatus("La cuenta ya fue eliminada");
        }
        this.status = AccountStatus.DELETED;
        this.deletedAt = now;
        this.deactivatedAt = null;
        this.email = "deleted-" + UUID.randomUUID() + DELETED_EMAIL_DOMAIN;
        this.alias = null;
        this.passwordHash = DISABLED_PASSWORD;
    }

    /** Name shown to other users: the alias, or {@code fallback} (e.g. the business name) when there is none. */
    public String displayName(String fallback) {
        return alias != null ? alias : fallback;
    }

    private void activate() {
        this.status = AccountStatus.ACTIVE;
        this.deactivatedAt = null;
    }

    private void requireOtherActiveAdministrator(UserAccount administrator) {
        if (administrator == this || (id != null && id.equals(administrator.getId()))) {
            throw new ForbiddenException("No puedes cambiar el estado de tu propia cuenta");
        }
        if (!administrator.isAdministrator() || !administrator.isActive()) {
            throw new ForbiddenException();
        }
    }

    private static String validAlias(Role role, String alias) {
        return role == Role.CUSTOMER
                ? Validators.text("alias", alias, Validators.ALIAS_MAX, "tu alias")
                : Validators.optionalText("alias", alias, Validators.ALIAS_MAX, "tu alias");
    }
}

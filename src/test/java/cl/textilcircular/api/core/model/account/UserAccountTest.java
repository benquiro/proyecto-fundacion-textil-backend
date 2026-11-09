package cl.textilcircular.api.core.model.account;

import cl.textilcircular.api.core.error.ConflictException;
import cl.textilcircular.api.core.error.ForbiddenException;
import cl.textilcircular.api.core.error.ValidationException;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UserAccountTest {

    static final PasswordEncoder ENCODER = new BCryptPasswordEncoder(4);
    static final Instant NOW = Instant.parse("2026-11-09T12:00:00Z");

    static UserAccount customer() {
        return UserAccount.registerCustomer(" Ana@Mail.CL ", "Demo1234", " Ana ", ENCODER, NOW);
    }

    @Test
    void customerRegistrationNormalizesAndHashes() {
        UserAccount account = customer();

        assertThat(account.getEmail()).isEqualTo("ana@mail.cl");
        assertThat(account.getAlias()).isEqualTo("Ana");
        assertThat(account.getRole()).isEqualTo(Role.CUSTOMER);
        assertThat(account.getStatus()).isEqualTo(AccountStatus.ACTIVE);
        assertThat(account.getConsentAt()).isEqualTo(NOW);
        assertThat(account.getPasswordHash()).isNotEqualTo("Demo1234");
        assertThat(ENCODER.matches("Demo1234", account.getPasswordHash())).isTrue();
    }

    @Test
    void customerNeedsAnAliasButEntrepreneurDoesNot() {
        assertThatThrownBy(() -> UserAccount.registerCustomer("a@b.cl", "Demo1234", " ", ENCODER, NOW))
                .isInstanceOf(ValidationException.class).extracting("field").isEqualTo("alias");

        UserAccount entrepreneur = UserAccount.registerEntrepreneur("e@b.cl", "Demo1234", null, ENCODER, NOW);
        assertThat(entrepreneur.getRole()).isEqualTo(Role.ENTREPRENEUR);
        assertThat(entrepreneur.getAlias()).isNull();
    }

    @Test
    void weakPasswordIsRejected() {
        assertThatThrownBy(() -> UserAccount.registerCustomer("a@b.cl", "password", "Ana", ENCODER, NOW))
                .isInstanceOf(ValidationException.class).extracting("field").isEqualTo("password");
    }

    @Test
    void wrongPasswordFailsWithInvalidCredentials() {
        assertThatThrownBy(() -> customer().authenticate("Other1234", ENCODER, NOW))
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessage("Correo o contraseña incorrectos");
    }

    @Test
    void suspendedAccountCannotLogIn() {
        UserAccount account = customer();
        account.suspendBy(admin());

        assertThatThrownBy(() -> account.authenticate("Demo1234", ENCODER, NOW))
                .isInstanceOf(AccountNotActiveException.class)
                .hasMessage("Tu cuenta está suspendida");
    }

    @Test
    void loggingInReactivatesADeactivatedAccount() {
        UserAccount account = customer();
        account.deactivate(NOW);
        assertThat(account.getDeactivatedAt()).isEqualTo(NOW);

        account.authenticate("Demo1234", ENCODER, NOW.plusSeconds(60));

        assertThat(account.getStatus()).isEqualTo(AccountStatus.ACTIVE);
        assertThat(account.getDeactivatedAt()).isNull();
    }

    @Test
    void onlyAnotherActiveAdministratorCanSuspend() {
        UserAccount admin = admin();
        assertThatThrownBy(() -> admin.suspendBy(admin))
                .isInstanceOf(ForbiddenException.class)
                .hasMessage("No puedes cambiar el estado de tu propia cuenta");
        assertThatThrownBy(() -> customer().suspendBy(customer()))
                .isInstanceOf(ForbiddenException.class);

        UserAccount account = customer();
        account.suspendBy(admin);
        assertThat(account.getStatus()).isEqualTo(AccountStatus.SUSPENDED);
        assertThatThrownBy(() -> account.suspendBy(admin)).isInstanceOf(ConflictException.class);

        account.reactivateBy(admin);
        assertThat(account.getStatus()).isEqualTo(AccountStatus.ACTIVE);
    }

    @Test
    void deletionAnonymizesAndFreesTheEmail() {
        UserAccount account = customer();

        account.delete(NOW);

        assertThat(account.getStatus()).isEqualTo(AccountStatus.DELETED);
        assertThat(account.getDeletedAt()).isEqualTo(NOW);
        assertThat(account.getEmail()).isNotEqualTo("ana@mail.cl").endsWith("@anonymized.invalid");
        assertThat(account.getAlias()).isNull();
        assertThatThrownBy(() -> account.authenticate("Demo1234", ENCODER, NOW))
                .isInstanceOf(InvalidCredentialsException.class);
        assertThatThrownBy(() -> account.delete(NOW)).isInstanceOf(ConflictException.class);
    }

    @Test
    void ensureActiveRejectsInactiveAccounts() {
        UserAccount account = customer();
        account.ensureActive();
        account.deactivate(NOW);

        assertThatThrownBy(account::ensureActive)
                .isInstanceOf(AccountNotActiveException.class)
                .hasMessage("Tu cuenta está desactivada");
    }

    @Test
    void passwordChangeNeedsTheCurrentOneAndANewValue() {
        UserAccount account = customer();

        assertThatThrownBy(() -> account.changePassword("Wrong1234", "Nueva1234", ENCODER))
                .isInstanceOf(ValidationException.class).extracting("field").isEqualTo("currentPassword");
        assertThatThrownBy(() -> account.changePassword("Demo1234", "Demo1234", ENCODER))
                .isInstanceOf(ValidationException.class).extracting("field").isEqualTo("newPassword");

        account.changePassword("Demo1234", "Nueva1234", ENCODER);
        assertThat(ENCODER.matches("Nueva1234", account.getPasswordHash())).isTrue();
    }

    @Test
    void profileUpdateRevalidatesEmailAndAlias() {
        UserAccount account = customer();

        account.updateProfile(" NEW@mail.cl ", "Anita");

        assertThat(account.getEmail()).isEqualTo("new@mail.cl");
        assertThat(account.getAlias()).isEqualTo("Anita");
        assertThatThrownBy(() -> account.updateProfile("new@mail.cl", "")).isInstanceOf(ValidationException.class);
    }

    @Test
    void inactiveAccountsCannotBeEdited() {
        UserAccount suspended = customer();
        suspended.suspendBy(admin());
        assertThatThrownBy(() -> suspended.updateProfile("x@b.cl", "X")).isInstanceOf(AccountNotActiveException.class);
        assertThatThrownBy(() -> suspended.changePassword("Demo1234", "Nueva1234", ENCODER))
                .isInstanceOf(AccountNotActiveException.class);

        UserAccount deleted = customer();
        deleted.delete(NOW);
        assertThatThrownBy(() -> deleted.resetPassword("Nueva1234", ENCODER)).isInstanceOf(ConflictException.class);
    }

    @Test
    void reactivationRulesMirrorSuspension() {
        UserAccount account = customer();
        UserAccount admin = admin();

        assertThatThrownBy(() -> account.reactivateBy(admin)).isInstanceOf(ConflictException.class);
        assertThatThrownBy(() -> admin.reactivateBy(admin)).isInstanceOf(ForbiddenException.class);

        UserAccount suspendedAdmin = admin();
        suspendedAdmin.suspendBy(admin);
        assertThatThrownBy(() -> account.suspendBy(suspendedAdmin)).isInstanceOf(ForbiddenException.class);

        account.deactivate(NOW);
        account.reactivateBy(admin);
        assertThat(account.getStatus()).isEqualTo(AccountStatus.ACTIVE);
        assertThat(account.getDeactivatedAt()).isNull();

        account.delete(NOW);
        assertThatThrownBy(() -> account.reactivateBy(admin)).isInstanceOf(ConflictException.class);
    }

    static UserAccount admin() {
        return UserAccount.registerAdministrator("admin@tc.cl", "Demo1234", "Admin", ENCODER, NOW);
    }
}

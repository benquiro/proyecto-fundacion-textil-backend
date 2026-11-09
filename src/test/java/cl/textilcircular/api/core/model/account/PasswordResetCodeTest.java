package cl.textilcircular.api.core.model.account;

import cl.textilcircular.api.core.error.ValidationException;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PasswordResetCodeTest {

    static final Instant NOW = Instant.parse("2026-11-09T12:00:00Z");

    PasswordResetCode.Issued issued = PasswordResetCode.issue(UserAccountTest.customer(), NOW, Duration.ofMinutes(15));

    @Test
    void codeIsSixDigitsAndStoredHashed() {
        assertThat(issued.rawCode()).matches("\\d{6}");
        assertThat(issued.code().getCodeHash()).isNotEqualTo(issued.rawCode());
    }

    @Test
    void correctCodeIsUsedOnce() {
        issued.code().redeem(issued.rawCode(), NOW.plusSeconds(60));

        assertThat(issued.code().getUsedAt()).isEqualTo(NOW.plusSeconds(60));
        assertThatThrownBy(() -> issued.code().redeem(issued.rawCode(), NOW.plusSeconds(61)))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void expiredCodeIsRejected() {
        assertThatThrownBy(() -> issued.code().redeem(issued.rawCode(), NOW.plus(Duration.ofMinutes(16))))
                .isInstanceOf(ValidationException.class)
                .hasMessage("El código expiró o ya no es válido");
    }

    @Test
    void codeExpiresExactlyAtItsDeadline() {
        assertThatThrownBy(() -> issued.code().redeem(issued.rawCode(), NOW.plus(Duration.ofMinutes(15))))
                .hasMessage("El código expiró o ya no es válido");
    }

    @Test
    void missingCodeCountsAsAWrongAttempt() {
        assertThatThrownBy(() -> issued.code().redeem(null, NOW)).hasMessage("El código no es correcto");
        assertThat(issued.code().getFailedAttempts()).isEqualTo(1);
    }

    @Test
    void codeLocksAfterFiveWrongAttempts() {
        String wrong = issued.rawCode().equals("000000") ? "111111" : "000000";
        for (int i = 0; i < 5; i++) {
            assertThatThrownBy(() -> issued.code().redeem(wrong, NOW)).hasMessage("El código no es correcto");
        }

        assertThat(issued.code().getFailedAttempts()).isEqualTo(PasswordResetCode.MAX_ATTEMPTS);
        assertThatThrownBy(() -> issued.code().redeem(issued.rawCode(), NOW))
                .hasMessage("El código expiró o ya no es válido");
        assertThat(issued.code().getFailedAttempts()).isEqualTo(PasswordResetCode.MAX_ATTEMPTS);
    }
}

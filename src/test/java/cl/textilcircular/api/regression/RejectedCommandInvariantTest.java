package cl.textilcircular.api.regression;

import cl.textilcircular.api.account.AccountNotActiveException;
import cl.textilcircular.api.account.Entrepreneur;
import cl.textilcircular.api.account.UserAccount;
import cl.textilcircular.api.business.Business;
import cl.textilcircular.api.business.BusinessStatus;
import cl.textilcircular.api.business.RejectionReason;
import cl.textilcircular.api.shared.error.ValidationException;
import cl.textilcircular.api.shared.validation.Validators;
import org.junit.jupiter.api.Test;

import static cl.textilcircular.api.support.Fixtures.NOW;
import static cl.textilcircular.api.support.Fixtures.ageRange;
import static cl.textilcircular.api.support.Fixtures.administrator;
import static cl.textilcircular.api.support.Fixtures.customer;
import static cl.textilcircular.api.support.Fixtures.entrepreneur;
import static cl.textilcircular.api.support.Fixtures.gender;
import static cl.textilcircular.api.support.Fixtures.publicBusiness;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Verifier probes: a command that throws must leave the entity exactly as it was. */
class RejectedCommandInvariantTest {

    @Test
    void rejectedProfileUpdateKeepsTheEmail() {
        UserAccount account = customer();
        String email = account.getEmail();

        assertThatThrownBy(() -> account.updateProfile("nuevo@b.cl", " "))
                .isInstanceOf(ValidationException.class);

        assertThat(account.getEmail()).isEqualTo(email);
    }

    @Test
    void rejectedEntrepreneurProfileUpdateKeepsTheFirstName() {
        Entrepreneur entrepreneur = entrepreneur();

        assertThatThrownBy(() -> entrepreneur.updateProfile("Beatriz", " ", ageRange(3L), gender(1L)))
                .isInstanceOf(ValidationException.class);

        assertThat(entrepreneur.getFirstName()).isEqualTo("Ana");
    }

    @Test
    void rejectedTakeDownKeepsTheReviewRound() {
        Business business = publicBusiness(entrepreneur());
        int round = business.getReviewRound();
        int reviews = business.getReviews().size();

        assertThatThrownBy(() -> business.takeDownBy(administrator(), RejectionReason.OTHER, " ", NOW))
                .isInstanceOf(ValidationException.class);
        assertThatThrownBy(() -> business.takeDownBy(administrator(), null, "Motivo", NOW))
                .isInstanceOf(ValidationException.class);

        assertThat(business.getStatus()).isEqualTo(BusinessStatus.APPROVED);
        assertThat(business.getReviews()).hasSize(reviews);
        assertThat(business.getReviewRound()).isEqualTo(round);
    }

    @Test
    void suspendedEntrepreneurCannotEditTheProfile() {
        Entrepreneur entrepreneur = entrepreneur();
        entrepreneur.getAccount().suspendBy(administrator());

        assertThatThrownBy(() -> entrepreneur.updateProfile("Beatriz", "Rojas", ageRange(3L), gender(1L)))
                .isInstanceOf(AccountNotActiveException.class);
        assertThat(entrepreneur.getFirstName()).isEqualTo("Ana");
    }

    @Test
    void passwordOverSeventyTwoBytesIsAValidationError() {
        String tooLong = "Aa1" + "x".repeat(70);

        assertThatThrownBy(() -> Validators.password(tooLong))
                .isInstanceOf(ValidationException.class)
                .hasMessage("La contraseña admite hasta 72 caracteres");
        assertThat(Validators.password("Aa1" + "x".repeat(69))).hasSize(72);
    }

    @Test
    void mediaPathRejectsDotSegments() {
        assertThatThrownBy(() -> Validators.url("photoUrl", "/media/..", true, "la foto"))
                .isInstanceOf(ValidationException.class);
        assertThatThrownBy(() -> Validators.url("photoUrl", "/media/.hidden.png", true, "la foto"))
                .isInstanceOf(ValidationException.class);
        assertThat(Validators.url("photoUrl", "/media/0b9f-12.webp", true, "la foto")).isEqualTo("/media/0b9f-12.webp");
    }
}

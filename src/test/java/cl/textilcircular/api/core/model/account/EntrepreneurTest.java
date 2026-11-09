package cl.textilcircular.api.core.model.account;

import cl.textilcircular.api.core.model.reference.AgeRange;
import cl.textilcircular.api.core.model.reference.Gender;
import cl.textilcircular.api.core.error.ValidationException;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

class EntrepreneurTest {

    static final Instant NOW = Instant.parse("2026-11-09T12:00:00Z");

    UserAccount account = UserAccount.registerEntrepreneur("e@b.cl", "Demo1234", null, UserAccountTest.ENCODER, NOW);
    AgeRange ageRange = mock(AgeRange.class);
    Gender gender = mock(Gender.class);

    @Test
    void profileNormalizesRutAndNames() {
        Entrepreneur entrepreneur = Entrepreneur.create(account, "12.345.678-5", " Ana ", " Pérez ", ageRange, gender);

        assertThat(entrepreneur.getRut()).isEqualTo("12345678-5");
        assertThat(entrepreneur.getFirstName()).isEqualTo("Ana");
        assertThat(entrepreneur.getLastName()).isEqualTo("Pérez");
        assertThat(entrepreneur.getAccount()).isSameAs(account);
    }

    @Test
    void onlyEntrepreneurAccountsGetAProfile() {
        UserAccount customer = UserAccountTest.customer();

        assertThatThrownBy(() -> Entrepreneur.create(customer, "12345678-5", "Ana", "Pérez", ageRange, gender))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void namesAndDemographicsAreRequired() {
        assertThatThrownBy(() -> Entrepreneur.create(account, "12345678-5", "", "Pérez", ageRange, gender))
                .isInstanceOf(ValidationException.class).extracting("field").isEqualTo("firstName");
        assertThatThrownBy(() -> Entrepreneur.create(account, "12345678-5", "Ana", "Pérez", null, gender))
                .isInstanceOf(ValidationException.class).extracting("field").isEqualTo("ageRangeId");
    }

    @Test
    void profileUpdateKeepsTheRut() {
        Entrepreneur entrepreneur = Entrepreneur.create(account, "12345678-5", "Ana", "Pérez", ageRange, gender);

        entrepreneur.updateProfile("María", "Soto", ageRange, gender);

        assertThat(entrepreneur.getFirstName()).isEqualTo("María");
        assertThat(entrepreneur.getRut()).isEqualTo("12345678-5");
    }

    @Test
    void anonymizationRemovesPersonalData() {
        Entrepreneur entrepreneur = Entrepreneur.create(account, "12345678-5", "Ana", "Pérez", ageRange, gender);

        entrepreneur.anonymize(NOW);

        assertThat(entrepreneur.getRut()).isNull();
        assertThat(entrepreneur.getFirstName()).isNull();
        assertThat(entrepreneur.getLastName()).isNull();
        assertThat(entrepreneur.getAnonymizedAt()).isEqualTo(NOW);
    }
}

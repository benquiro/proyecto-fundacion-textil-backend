package cl.textilcircular.api.account;

import cl.textilcircular.api.reference.AgeRange;
import cl.textilcircular.api.reference.Gender;
import cl.textilcircular.api.support.RepositoryTest;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@RepositoryTest
class AccountRepositoriesTest {

    static final Instant NOW = Instant.parse("2026-11-09T12:00:00Z");

    @Autowired UserAccountRepository accounts;
    @Autowired EntrepreneurRepository entrepreneurs;
    @Autowired RefreshTokenRepository refreshTokens;
    @Autowired PasswordResetCodeRepository resetCodes;
    @Autowired EntityManager em;

    UserAccount saveCustomer(String email) {
        return accounts.saveAndFlush(UserAccount.registerCustomer(email, "Demo1234", "Ana", UserAccountTest.ENCODER, NOW));
    }

    @Test
    void accountIsFoundByNormalizedEmail() {
        UserAccount saved = saveCustomer("Ana@Mail.cl");

        assertThat(saved.getId()).isGreaterThanOrEqualTo(951L);
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(accounts.findByEmail("ana@mail.cl")).contains(saved);
        assertThat(accounts.existsByEmail("ana@mail.cl")).isTrue();
    }

    @Test
    void databaseRejectsADuplicateEmail() {
        saveCustomer("ana@mail.cl");

        assertThatThrownBy(() -> saveCustomer("ana@mail.cl"))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void databaseRejectsAStatusWithoutItsTimestamp() {
        UserAccount saved = saveCustomer("ana@mail.cl");

        assertThatThrownBy(() -> {
            em.createNativeQuery("UPDATE user_account SET status = 'DELETED' WHERE user_id = :id")
                    .setParameter("id", saved.getId()).executeUpdate();
        }).rootCause().hasMessageContaining("ck_user_account_deleted");
    }

    @Test
    void entrepreneurSharesTheAccountIdAndItsRutIsUnique() {
        UserAccount account = accounts.save(UserAccount.registerEntrepreneur("e@b.cl", "Demo1234", null, UserAccountTest.ENCODER, NOW));
        Entrepreneur saved = entrepreneurs.saveAndFlush(Entrepreneur.create(account, "12345678-5", "Ana", "Pérez",
                em.getReference(AgeRange.class, 3L), em.getReference(Gender.class, 1L)));

        assertThat(saved.getId()).isEqualTo(account.getId());
        assertThat(entrepreneurs.existsByRut("12345678-5")).isTrue();

        UserAccount other = accounts.save(UserAccount.registerEntrepreneur("f@b.cl", "Demo1234", null, UserAccountTest.ENCODER, NOW));
        assertThatThrownBy(() -> entrepreneurs.saveAndFlush(Entrepreneur.create(other, "12345678-5", "Eva", "Rojas",
                em.getReference(AgeRange.class, 3L), em.getReference(Gender.class, 1L))))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void deletedAccountKeepsAnAnonymizedProfile() {
        UserAccount account = accounts.save(UserAccount.registerEntrepreneur("e@b.cl", "Demo1234", null, UserAccountTest.ENCODER, NOW));
        Entrepreneur entrepreneur = entrepreneurs.save(Entrepreneur.create(account, "12345678-5", "Ana", "Pérez",
                em.getReference(AgeRange.class, 3L), em.getReference(Gender.class, 1L)));
        accounts.flush();

        account.delete(NOW);
        entrepreneur.anonymize(NOW);
        accounts.flush();
        em.clear();

        Entrepreneur reloaded = entrepreneurs.findById(account.getId()).orElseThrow();
        assertThat(reloaded.getRut()).isNull();
        assertThat(reloaded.getAccount().getStatus()).isEqualTo(AccountStatus.DELETED);
        assertThat(accounts.existsByEmail("e@b.cl")).isFalse();
    }

    @Test
    void refreshTokensAreStoredHashedAndRevokedTogether() {
        UserAccount account = saveCustomer("ana@mail.cl");
        RefreshToken.Issued first = RefreshToken.issue(account, NOW, Duration.ofDays(30));
        RefreshToken.Issued second = RefreshToken.issue(account, NOW, Duration.ofDays(30));
        refreshTokens.saveAll(List.of(first.token(), second.token()));
        refreshTokens.flush();

        RefreshToken found = refreshTokens.findByTokenHash(RefreshToken.hash(first.rawToken())).orElseThrow();
        assertThat(found.getTokenHash()).isNotEqualTo(first.rawToken());
        assertThat(found.isActive(NOW)).isTrue();

        int revoked = refreshTokens.revokeAllActive(account.getId(), NOW);
        em.clear();

        assertThat(revoked).isEqualTo(2);
        assertThat(refreshTokens.findByTokenHash(RefreshToken.hash(second.rawToken())).orElseThrow().isActive(NOW)).isFalse();
    }

    @Test
    void resetCodeIsFoundAmongTheActiveOnes() {
        UserAccount account = saveCustomer("ana@mail.cl");
        PasswordResetCode.Issued issued = PasswordResetCode.issue(account, NOW, Duration.ofMinutes(15));
        resetCodes.saveAndFlush(issued.code());

        assertThat(resetCodes.findFirstByAccountIdAndUsedAtIsNullOrderByCreatedAtDesc(account.getId()))
                .contains(issued.code());
    }
}

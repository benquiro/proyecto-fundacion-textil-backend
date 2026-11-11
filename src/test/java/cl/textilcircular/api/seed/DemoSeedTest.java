package cl.textilcircular.api.seed;

import cl.textilcircular.api.core.model.account.UserAccount;
import cl.textilcircular.api.core.repository.UserAccountRepository;
import cl.textilcircular.api.core.repository.BusinessRepository;
import cl.textilcircular.api.core.model.business.BusinessReview;
import cl.textilcircular.api.core.model.business.BusinessStatus;
import cl.textilcircular.api.core.model.business.CatalogEntry;
import cl.textilcircular.api.core.repository.CatalogEntryRepository;
import cl.textilcircular.api.core.model.business.RejectionReason;
import cl.textilcircular.api.core.repository.ConversationRepository;
import cl.textilcircular.api.core.repository.OutreachContentRepository;
import cl.textilcircular.api.core.repository.ProductRepository;
import cl.textilcircular.api.support.RepositoryTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.test.context.TestPropertySource;

import javax.sql.DataSource;
import java.sql.Connection;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

/** Loads the dev profile migrations (schema + demo data) into its own database. */
@RepositoryTest
@TestPropertySource(properties = "spring.flyway.locations=classpath:db/migration,classpath:db/seed-dev")
class DemoSeedTest {

    @Autowired UserAccountRepository accounts;
    @Autowired BusinessRepository businesses;
    @Autowired CatalogEntryRepository catalog;
    @Autowired ProductRepository products;
    @Autowired ConversationRepository conversations;
    @Autowired OutreachContentRepository contents;
    @Autowired JdbcTemplate jdbc;
    @Autowired DataSource dataSource;

    @Test
    void demoAccountsLogInWithTheDocumentedPassword() {
        assertThat(accounts.count()).isEqualTo(11);
        UserAccount martin = accounts.findByEmail("martin.diaz@example.cl").orElseThrow();

        martin.authenticate("Demo1234", PasswordEncoderFactories.createDelegatingPasswordEncoder(), Instant.now());

        assertThat(martin.getPasswordHash()).startsWith("{bcrypt}");
    }

    @Test
    void directoryShowsTheThreeApprovedBusinesses() {
        assertThat(catalog.search(null, null, null, null, "demo", PageRequest.of(0, 20)).getContent())
                .extracting(CatalogEntry::getId).containsExactlyInAnyOrder(1L, 2L, 3L);
        assertThat(businesses.findById(4L).orElseThrow().getStatus()).isEqualTo(BusinessStatus.PENDING);
        assertThat(products.findPublicByBusinessId(2L)).hasSize(2);
    }

    @Test
    void seededRejectionCarriesAStructuredReason() {
        BusinessReview rejection = businesses.findById(5L).orElseThrow().getReviews().getFirst();

        assertThat(rejection.getRejectionReason()).isEqualTo(RejectionReason.UNCLEAR_DESCRIPTION);
    }

    @Test
    void chatsAndFeedAreAvailable() {
        assertThat(conversations.findInbox(8L)).hasSize(2);
        assertThat(contents.findByDeletedAtIsNullOrderByCreatedAtDescIdDesc(PageRequest.of(0, 20)).getTotalElements()).isEqualTo(5);
    }

    @Test
    void runningTheSeedAgainChangesNothing() throws Exception {
        String counts = "SELECT (SELECT count(*) FROM user_account) || '/' || (SELECT count(*) FROM business_service)"
                + " || '/' || (SELECT count(*) FROM message) || '/' || (SELECT count(*) FROM product_photo)";
        String before = jdbc.queryForObject(counts, String.class);

        try (Connection connection = dataSource.getConnection()) {
            ScriptUtils.executeSqlScript(connection, new ClassPathResource("db/seed-dev/R__demo_data.sql"));
        }

        assertThat(jdbc.queryForObject(counts, String.class)).isEqualTo(before);
    }
}

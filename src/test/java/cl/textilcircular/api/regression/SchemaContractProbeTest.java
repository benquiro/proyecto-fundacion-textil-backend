package cl.textilcircular.api.regression;

import cl.textilcircular.api.core.model.account.UserAccount;
import cl.textilcircular.api.core.model.business.Business;
import cl.textilcircular.api.core.model.business.CatalogEntry;
import cl.textilcircular.api.core.repository.CatalogEntryRepository;
import cl.textilcircular.api.core.validation.Validators;
import cl.textilcircular.api.support.PersistedData;
import cl.textilcircular.api.support.RepositoryTest;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Verifier probes of the schema contract (S4) against the real PostgreSQL schema. */
@RepositoryTest
class SchemaContractProbeTest {

    @Autowired EntityManager em;
    @Autowired JdbcTemplate jdbc;
    @Autowired CatalogEntryRepository catalog;
    PersistedData data;

    @BeforeEach
    void setUp() {
        data = new PersistedData(em);
    }

    @Test
    void databaseColumnsCarryTheAppMaxLengths() {
        Map<String, Integer> expected = Map.ofEntries(
                Map.entry("user_account.alias", Validators.ALIAS_MAX),
                Map.entry("user_account.email", Validators.EMAIL_MAX),
                Map.entry("entrepreneur.first_name", Validators.PERSON_NAME_MAX),
                Map.entry("entrepreneur.last_name", Validators.PERSON_NAME_MAX),
                Map.entry("business.name", Validators.BUSINESS_NAME_MAX),
                Map.entry("business.description", Validators.BUSINESS_DESCRIPTION_MAX),
                Map.entry("business.photo_url", Validators.URL_MAX),
                Map.entry("product.title", Validators.PRODUCT_TITLE_MAX),
                Map.entry("product.description", Validators.PRODUCT_DESCRIPTION_MAX),
                Map.entry("product_photo.url", Validators.URL_MAX),
                Map.entry("message.content", Validators.MESSAGE_MAX),
                Map.entry("business_review.comment", Validators.COMMENT_MAX),
                Map.entry("conversation_report.detail", Validators.COMMENT_MAX),
                Map.entry("outreach_content.title", Validators.CONTENT_TITLE_MAX),
                Map.entry("outreach_content.body", Validators.CONTENT_BODY_MAX),
                Map.entry("outreach_content.image_url", Validators.URL_MAX),
                Map.entry("outreach_content.event_location", Validators.EVENT_LOCATION_MAX));

        expected.forEach((column, max) -> {
            String[] parts = column.split("\\.");
            Integer actual = jdbc.queryForObject("""
                    SELECT character_maximum_length FROM information_schema.columns
                    WHERE table_schema = 'public' AND table_name = ? AND column_name = ?""",
                    Integer.class, parts[0], parts[1]);
            assertThat(actual).as(column).isEqualTo(max);
        });
    }

    @Test
    void databaseRejectsATooLongMessage() {
        Business business = data.approvedBusiness("Taller Largo", "Desc", 102L, 1L);
        UserAccount customer = data.customer();
        data.flushAndClear();
        jdbc.update("INSERT INTO conversation (conversation_id, initiator_id, business_id, created_at) VALUES (900, ?, ?, now())",
                customer.getId(), business.getId());

        assertThatThrownBy(() -> jdbc.update("""
                INSERT INTO message (message_id, conversation_id, sender_id, client_uuid, content, sent_at)
                VALUES (900, 900, ?, gen_random_uuid(), repeat('x', 2001), now())""", customer.getId()))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void databaseRejectsARejectedVoteWithoutAComment() {
        Business business = data.pendingBusiness("Taller Voto", "Desc", 102L, 1L);
        UserAccount admin = data.administrator();
        data.flushAndClear();

        assertThatThrownBy(() -> jdbc.update("""
                INSERT INTO business_review (review_id, business_id, reviewer_id, review_round, decision, rejection_reason, comment, reviewed_at)
                VALUES (900, ?, ?, 1, 'REJECTED', 'OTHER', '  ', now())""", business.getId(), admin.getId()))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void catalogViewExposesNoPersonalColumns() {
        List<String> columns = jdbc.queryForList("""
                SELECT column_name FROM information_schema.columns
                WHERE table_schema = 'public' AND table_name = 'v_catalog'""", String.class);

        assertThat(columns).containsExactlyInAnyOrder("business_id", "name", "description", "photo_url",
                "commune_id", "commune_name", "region_id", "region_name", "approved_at", "search_text");
    }

    @Test
    void catalogDropsABusinessWhoseOwnerIsSuspended() {
        Business business = data.approvedBusiness("Taller Suspendido", "Desc", 102L, 1L);
        UserAccount admin = data.administrator();
        business.getOwner().getAccount().suspendBy(admin);
        data.flushAndClear();

        assertThat(catalog.findById(business.getId())).isEmpty();
    }

    @Test
    void likeWildcardsInTheTermAreLiteral() {
        data.approvedBusiness("Taller Cien", "Arreglos al 100% de calidad", 102L, 1L);
        data.approvedBusiness("Taller Otro", "Costura simple", 102L, 1L);
        data.flushAndClear();

        assertThat(catalog.search("%", null, null, null, "s", PageRequest.of(0, 50)).getContent())
                .extracting(CatalogEntry::getName).containsExactly("Taller Cien");
        assertThat(catalog.search("_", null, null, null, "s", PageRequest.of(0, 50)).getContent()).isEmpty();
        assertThat(catalog.search("ÁRREGLOS", null, null, null, "s", PageRequest.of(0, 50)).getContent())
                .extracting(CatalogEntry::getName).containsExactly("Taller Cien");
    }

    @Test
    void rejectedTakeDownLeavesTheStoredRoundUnchanged() {
        Business business = data.approvedBusiness("Taller Moderado", "Desc", 102L, 1L);
        UserAccount admin = data.administrator();
        data.flushAndClear();
        Business loaded = em.find(Business.class, business.getId());
        UserAccount moderator = em.find(UserAccount.class, admin.getId());

        assertThatThrownBy(() -> loaded.takeDownBy(moderator, cl.textilcircular.api.core.model.business.RejectionReason.OTHER,
                " ", java.time.Instant.now())).isInstanceOf(cl.textilcircular.api.core.error.ValidationException.class);
        em.flush();

        assertThat(jdbc.queryForObject("SELECT review_round FROM business WHERE business_id = ?", Integer.class,
                business.getId())).isEqualTo(1);
    }

    /** Lists foreign keys whose columns are not the leading columns of any index (S4 "explicit FK indexes"). */
    @Test
    void everyForeignKeyHasAnIndex() {
        List<String> unindexed = jdbc.queryForList("""
                SELECT c.conrelid::regclass || '.' || a.attname
                FROM pg_constraint c
                JOIN pg_attribute a ON a.attrelid = c.conrelid AND a.attnum = c.conkey[1]
                WHERE c.contype = 'f' AND array_length(c.conkey, 1) = 1
                  AND NOT EXISTS (SELECT 1 FROM pg_index i
                                  WHERE i.indrelid = c.conrelid AND i.indkey[0] = c.conkey[1])
                ORDER BY 1""", String.class);

        assertThat(unindexed).isEmpty();
    }
}

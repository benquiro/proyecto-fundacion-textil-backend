package cl.textilcircular.api.core.repository;

import cl.textilcircular.api.core.model.outreach.ContentType;
import cl.textilcircular.api.core.model.outreach.OutreachContent;
import cl.textilcircular.api.core.model.account.UserAccount;
import cl.textilcircular.api.core.model.analytics.AnalyticsEvent;
import cl.textilcircular.api.core.repository.AnalyticsEventRepository;
import cl.textilcircular.api.core.model.analytics.AnalyticsEventType;
import cl.textilcircular.api.core.model.business.Business;
import cl.textilcircular.api.core.model.media.MediaFile;
import cl.textilcircular.api.core.repository.MediaFileRepository;
import cl.textilcircular.api.support.PersistedData;
import cl.textilcircular.api.support.RepositoryTest;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;

import java.time.Duration;

import static cl.textilcircular.api.support.Fixtures.NOW;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@RepositoryTest
class OutreachMediaAnalyticsRepositoriesTest {

    @Autowired OutreachContentRepository contents;
    @Autowired MediaFileRepository media;
    @Autowired AnalyticsEventRepository events;
    @Autowired EntityManager em;
    PersistedData data;
    UserAccount admin;

    @BeforeEach
    void setUp() {
        data = new PersistedData(em);
        admin = data.administrator();
    }

    @Test
    void feedIsNewestFirstWithoutDeletedContent() {
        OutreachContent first = contents.saveAndFlush(OutreachContent.publish(admin, ContentType.TEXT, null, "Primera", null, null, null));
        OutreachContent second = contents.saveAndFlush(OutreachContent.publish(admin, ContentType.TEXT, null, "Segunda", null, null, null));
        OutreachContent removed = contents.saveAndFlush(OutreachContent.publish(admin, ContentType.TEXT, null, "Borrada", null, null, null));
        removed.delete(admin, NOW);
        data.flushAndClear();

        assertThat(contents.findByDeletedAtIsNullOrderByCreatedAtDescIdDesc(PageRequest.of(0, 50)).getContent())
                .extracting(OutreachContent::getBody).containsSubsequence("Segunda", "Primera").doesNotContain("Borrada");
        assertThat(contents.findByIdAndDeletedAtIsNull(removed.getId())).isEmpty();
        assertThat(contents.findByIdAndDeletedAtIsNull(second.getId())).isPresent();
        assertThat(first.getCreatedAt()).isNotNull();
    }

    @Test
    void databaseEnforcesTypeSpecificFields() {
        OutreachContent text = contents.saveAndFlush(OutreachContent.publish(admin, ContentType.TEXT, null, "Hola", null, null, null));

        assertThatThrownBy(() -> em.createNativeQuery("UPDATE outreach_content SET type = 'EVENT' WHERE content_id = :id")
                .setParameter("id", text.getId()).executeUpdate())
                .rootCause().hasMessageContaining("ck_outreach_content_event");
    }

    @Test
    void mediaFileIsInsertedWithItsAssignedId() {
        MediaFile file = MediaFile.register(admin, "image/webp", 2048, NOW);

        media.saveAndFlush(file);
        data.flushAndClear();

        assertThat(media.findById(file.getId())).hasValueSatisfying(found -> {
            assertThat(found.getStoragePath()).endsWith(".webp");
            assertThat(found.isNew()).isFalse();
        });
    }

    @Test
    void eventsAreCountedPerTypeInAWindow() {
        Business business = data.approvedBusiness("Taller", "Desc", 102L, 1L);
        events.save(AnalyticsEvent.businessView(null, business, NOW));
        events.save(AnalyticsEvent.businessView(null, business, NOW.plus(Duration.ofDays(2))));
        events.save(AnalyticsEvent.directorySearch("bolso", null, null, null, NOW));
        em.flush();

        assertThat(events.countByTypeAndOccurredAtGreaterThanEqualAndOccurredAtLessThan(
                AnalyticsEventType.BUSINESS_VIEW, NOW, NOW.plus(Duration.ofDays(1)))).isEqualTo(1);
        assertThat(events.countByTypeAndOccurredAtGreaterThanEqualAndOccurredAtLessThan(
                AnalyticsEventType.DIRECTORY_SEARCH, NOW, NOW.plus(Duration.ofDays(1)))).isEqualTo(1);
    }

    @Test
    void databaseRequiresABusinessForAView() {
        assertThatThrownBy(() -> em.createNativeQuery(
                        "INSERT INTO analytics_event (event_id, type, occurred_at) VALUES (1, 'BUSINESS_VIEW', now())")
                .executeUpdate())
                .rootCause().hasMessageContaining("ck_analytics_event_target");
    }
}

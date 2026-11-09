package cl.textilcircular.api.core.repository;

import cl.textilcircular.api.core.model.business.Business;
import cl.textilcircular.api.core.model.business.BusinessStatus;
import cl.textilcircular.api.core.model.business.CatalogEntry;
import cl.textilcircular.api.core.model.business.ReviewDecision;
import cl.textilcircular.api.core.model.account.UserAccount;
import cl.textilcircular.api.core.model.reference.Commune;
import cl.textilcircular.api.core.model.reference.TextileService;
import cl.textilcircular.api.support.PersistedData;
import cl.textilcircular.api.support.RepositoryTest;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.util.List;

import static cl.textilcircular.api.support.Fixtures.NOW;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@RepositoryTest
class BusinessRepositoriesTest {

    @Autowired BusinessRepository businesses;
    @Autowired CatalogEntryRepository catalog;
    @Autowired EntityManager em;
    PersistedData data;

    @BeforeEach
    void setUp() {
        data = new PersistedData(em);
    }

    @Test
    void servicesKeepTheirOrderAndCanBeSwapped() {
        Business business = data.pendingBusiness("Taller", "Desc", 102L, 4L, 2L);
        data.flushAndClear();

        Business loaded = businesses.findById(business.getId()).orElseThrow();
        assertThat(loaded.getServices()).extracting(TextileService::getId).containsExactly(4L, 2L);

        loaded.edit("Taller", "Desc", null, loaded.getCommune(), data.services(2L, 4L), NOW);
        data.flushAndClear();
        assertThat(businesses.findById(business.getId()).orElseThrow().getServices())
                .extracting(TextileService::getId).containsExactly(2L, 4L);

        Business again = businesses.findById(business.getId()).orElseThrow();
        again.edit("Taller", "Desc", null, again.getCommune(), data.services(7L), NOW);
        data.flushAndClear();
        assertThat(businesses.findById(business.getId()).orElseThrow().getServices())
                .extracting(TextileService::getId).containsExactly(7L);
    }

    @Test
    void votesAndOutcomeArePersisted() {
        Business business = data.approvedBusiness("Taller", "Desc", 102L, 1L);
        data.flushAndClear();

        Business loaded = businesses.findByIdForUpdate(business.getId()).orElseThrow();
        assertThat(loaded.getStatus()).isEqualTo(BusinessStatus.APPROVED);
        assertThat(loaded.getReviews()).hasSize(2);
        assertThat(businesses.findByOwnerId(business.getOwner().getId())).contains(loaded);
        assertThat(businesses.existsByOwnerIdAndStatusAndDeletedAtIsNull(business.getOwner().getId(), BusinessStatus.APPROVED)).isTrue();
    }

    @Test
    void databaseRejectsApprovedWithoutApprovalDate() {
        Business business = data.pendingBusiness("Taller", "Desc", 102L, 1L);
        em.flush();

        assertThatThrownBy(() -> em.createNativeQuery("UPDATE business SET status = 'APPROVED' WHERE business_id = :id")
                .setParameter("id", business.getId()).executeUpdate())
                .rootCause().hasMessageContaining("ck_business_approved");
    }

    @Test
    void reviewQueueSkipsOwnBusinessAndAlreadyVoted() {
        Business mine = data.pendingBusiness("Mío", "Desc", 102L, 1L);
        Business voted = data.pendingBusiness("Votado", "Desc", 102L, 1L);
        Business open = data.pendingBusiness("Abierto", "Desc", 102L, 1L);
        data.approvedBusiness("Publicado", "Desc", 102L, 1L);
        UserAccount reviewer = mine.getOwner().getAccount();
        voted.vote(data.administrator(), ReviewDecision.APPROVED, null, null, NOW);
        voted.vote(reviewer, ReviewDecision.APPROVED, null, null, NOW);
        // 'voted' is now APPROVED (two votes); add one more pending that this reviewer voted on
        Business votedPending = data.pendingBusiness("Votado pendiente", "Desc", 102L, 1L);
        votedPending.vote(reviewer, ReviewDecision.APPROVED, null, null, NOW);
        data.flushAndClear();

        Page<Business> queue = businesses.findReviewQueue(reviewer.getId(), PageRequest.of(0, 20));

        assertThat(queue.getContent()).extracting(Business::getName).contains("Abierto")
                .doesNotContain("Mío", "Votado", "Votado pendiente", "Publicado");
        assertThat(open.getId()).isNotNull();
    }

    @Test
    void catalogShowsOnlyPublicBusinessesWithoutPersonalData() {
        Business visible = data.approvedBusiness("Hilos de Barrio", "Arreglos", 102L, 4L);
        data.pendingBusiness("Pendiente", "Desc", 102L, 4L);
        Business hidden = data.approvedBusiness("Oculto", "Desc", 102L, 4L);
        hidden.hide(NOW);
        Business suspendedOwner = data.approvedBusiness("Suspendida", "Desc", 102L, 4L);
        suspendedOwner.getOwner().getAccount().suspendBy(data.administrator());
        data.flushAndClear();

        List<String> names = catalog.search(null, null, null, null, "seed", PageRequest.of(0, 50))
                .map(CatalogEntry::getName).getContent();

        assertThat(names).contains("Hilos de Barrio").doesNotContain("Pendiente", "Oculto", "Suspendida");
        CatalogEntry entry = catalog.findById(visible.getId()).orElseThrow();
        assertThat(entry.getCommuneName()).isEqualTo(em.find(Commune.class, 102L).getName());
        assertThat(entry.getRegionName()).isNotBlank();
        assertThat(catalog.findById(hidden.getId())).isEmpty();
    }

    @Test
    void searchIgnoresAccentsAndCaseAndMatchesPartialWords() {
        data.approvedBusiness("Confección Circular", "Ropa a medida", 102L, 2L);
        data.approvedBusiness("Otro taller", "Teñido natural", 45L, 4L);
        data.flushAndClear();

        assertThat(names("confeccion", null, null, null)).containsExactly("Confección Circular");
        assertThat(names("TEÑIDO", null, null, null)).containsExactly("Otro taller");
        assertThat(names("medid", null, null, null)).containsExactly("Confección Circular");
        assertThat(names("100%", null, null, null)).isEmpty();
    }

    @Test
    void searchFiltersByRegionCommuneAndService() {
        data.approvedBusiness("Taller A", "Desc", 102L, 2L);
        data.approvedBusiness("Taller B", "Desc", 45L, 4L, 2L);
        data.flushAndClear();
        long regionOfB = em.find(Commune.class, 45L).getRegion().getId();

        assertThat(names("taller", null, 102L, null)).containsExactly("Taller A");
        assertThat(names("taller", regionOfB, null, null)).contains("Taller B").doesNotContain("Taller A");
        assertThat(names("taller", null, null, 4L)).containsExactly("Taller B");
        assertThat(names("taller", null, null, 2L)).containsExactlyInAnyOrder("Taller A", "Taller B");
    }

    @Test
    void shuffledOrderIsStableForTheSameSeed() {
        for (int i = 0; i < 6; i++) {
            data.approvedBusiness("Taller " + i, "Desc", 102L, 1L);
        }
        data.flushAndClear();

        List<Long> first = ids("abc");
        assertThat(ids("abc")).isEqualTo(first);
        assertThat(catalog.search(null, null, null, null, "abc", PageRequest.of(0, 2)).getTotalElements())
                .isEqualTo(first.size());
    }

    private List<String> names(String term, Long regionId, Long communeId, Long serviceId) {
        return catalog.search(term, regionId, communeId, serviceId, "seed", PageRequest.of(0, 50))
                .map(CatalogEntry::getName).getContent();
    }

    private List<Long> ids(String seed) {
        return catalog.search(null, null, null, null, seed, PageRequest.of(0, 100)).map(CatalogEntry::getId).getContent();
    }
}

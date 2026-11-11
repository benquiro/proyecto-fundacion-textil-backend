package cl.textilcircular.api.core.repository;

import cl.textilcircular.api.core.model.business.Business;
import cl.textilcircular.api.core.model.business.ReviewDecision;
import cl.textilcircular.api.core.model.account.UserAccount;
import cl.textilcircular.api.support.PersistedData;
import cl.textilcircular.api.support.RepositoryTest;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.TimeUnit;

import static cl.textilcircular.api.support.Fixtures.NOW;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Two reviewers approve at the same moment. Without the row lock each transaction would count
 * only its own vote and the business would stay PENDING with two approvals.
 */
@RepositoryTest
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class BusinessVoteConcurrencyTest {

    @Autowired BusinessRepository businesses;
    @Autowired EntityManager em;
    @Autowired PlatformTransactionManager transactionManager;
    @Autowired JdbcTemplate jdbc;

    Long businessId;
    Long ownerId;
    Long[] reviewerIds;

    @Test
    void simultaneousApprovalsPublishTheBusiness() throws Exception {
        TransactionTemplate tx = new TransactionTemplate(transactionManager);
        tx.executeWithoutResult(status -> {
            PersistedData data = new PersistedData(em);
            Business business = data.pendingBusiness("Concurrente", "Desc", 102L, 1L);
            UserAccount first = data.administrator();
            UserAccount second = data.administrator();
            businessId = business.getId();
            ownerId = business.getOwner().getId();
            reviewerIds = new Long[]{first.getId(), second.getId()};
        });

        CyclicBarrier bothLoaded = new CyclicBarrier(2);
        CompletableFuture<?>[] votes = new CompletableFuture<?>[2];
        for (int i = 0; i < 2; i++) {
            Long reviewerId = reviewerIds[i];
            votes[i] = CompletableFuture.runAsync(() -> tx.executeWithoutResult(status -> {
                await(bothLoaded);
                Business business = businesses.findByIdForUpdate(businessId).orElseThrow();
                business.vote(em.find(UserAccount.class, reviewerId), ReviewDecision.APPROVED, null, null, NOW);
            }));
        }
        CompletableFuture.allOf(votes).get(30, TimeUnit.SECONDS);

        String status = jdbc.queryForObject("SELECT status FROM business WHERE business_id = ?", String.class, businessId);
        Integer reviews = jdbc.queryForObject("SELECT count(*) FROM business_review WHERE business_id = ?", Integer.class, businessId);
        assertThat(reviews).isEqualTo(2);
        assertThat(status).isEqualTo("APPROVED");
    }

    @AfterEach
    void cleanUp() {
        if (businessId == null) {
            return;
        }
        jdbc.update("DELETE FROM business_review WHERE business_id = ?", businessId);
        jdbc.update("DELETE FROM business_service WHERE business_id = ?", businessId);
        jdbc.update("DELETE FROM business WHERE business_id = ?", businessId);
        jdbc.update("DELETE FROM entrepreneur WHERE user_id = ?", ownerId);
        jdbc.update("DELETE FROM user_account WHERE user_id IN (?, ?, ?)", ownerId, reviewerIds[0], reviewerIds[1]);
    }

    private static void await(CyclicBarrier barrier) {
        try {
            barrier.await(10, TimeUnit.SECONDS);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}

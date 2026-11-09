package cl.textilcircular.api.business;

import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface BusinessRepository extends JpaRepository<Business, Long> {

    /**
     * Loads the business holding its row lock until the transaction ends. Votes go through here so
     * two simultaneous approvals see each other and the second one publishes the business.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select b from Business b where b.id = :id")
    Optional<Business> findByIdForUpdate(Long id);

    Optional<Business> findByOwnerId(Long ownerId);

    /** Entrepreneur reviewers qualify only while their own business is approved. */
    boolean existsByOwnerIdAndStatusAndDeletedAtIsNull(Long ownerId, BusinessStatus status);

    /**
     * Pending businesses the reviewer can vote on now: not their own, owner active, and no vote of
     * theirs in the current round. Oldest change first.
     */
    @Query(value = """
            select b from Business b join b.owner o join o.account a
            where b.status = cl.textilcircular.api.business.BusinessStatus.PENDING
              and b.deletedAt is null
              and a.status = cl.textilcircular.api.account.AccountStatus.ACTIVE
              and o.id <> :reviewerId
              and not exists (select 1 from BusinessReview r
                              where r.business = b and r.reviewRound = b.reviewRound and r.reviewer.id = :reviewerId)
            order by b.updatedAt asc
            """)
    Page<Business> findReviewQueue(Long reviewerId, Pageable pageable);
}

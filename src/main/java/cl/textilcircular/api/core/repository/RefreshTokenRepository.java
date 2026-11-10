package cl.textilcircular.api.core.repository;

import cl.textilcircular.api.core.model.account.RefreshToken;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    Optional<RefreshToken> findByTokenHash(String tokenHash);

    /** Rotation goes through here, so two refreshes with the same token cannot both succeed. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select t from RefreshToken t where t.tokenHash = :tokenHash")
    Optional<RefreshToken> findByTokenHashForUpdate(String tokenHash);

    /**
     * Ends every session of a user (logout everywhere, password change, suspension, deletion).
     * Does not clear the persistence context, so changes to the account in the same transaction
     * are still saved; a RefreshToken already loaded keeps its old {@code revokedAt}.
     */
    @Modifying(flushAutomatically = true)
    @Query("update RefreshToken t set t.revokedAt = :now where t.account.id = :userId and t.revokedAt is null")
    int revokeAllActive(Long userId, Instant now);

    /** Ends every session of a user except {@code keepId} (password change keeps the caller signed in). */
    @Modifying(flushAutomatically = true)
    @Query("update RefreshToken t set t.revokedAt = :now where t.account.id = :userId and t.revokedAt is null and t.id <> :keepId")
    int revokeAllActiveExcept(Long userId, Long keepId, Instant now);
}

package cl.textilcircular.api.core.repository;

import cl.textilcircular.api.core.model.account.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    Optional<RefreshToken> findByTokenHash(String tokenHash);

    /** Ends every session of a user (logout everywhere, password change, suspension, deletion). */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("update RefreshToken t set t.revokedAt = :now where t.account.id = :userId and t.revokedAt is null")
    int revokeAllActive(Long userId, Instant now);
}

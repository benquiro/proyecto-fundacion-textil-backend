package cl.textilcircular.api.core.repository;

import cl.textilcircular.api.core.model.account.PasswordResetCode;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PasswordResetCodeRepository extends JpaRepository<PasswordResetCode, Long> {

    /** Latest unused code of the user; older codes are superseded by it. */
    Optional<PasswordResetCode> findFirstByAccountIdAndUsedAtIsNullOrderByCreatedAtDesc(Long userId);
}

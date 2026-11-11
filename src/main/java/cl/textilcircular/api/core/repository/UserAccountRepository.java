package cl.textilcircular.api.core.repository;

import cl.textilcircular.api.core.model.account.UserAccount;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserAccountRepository extends JpaRepository<UserAccount, Long> {

    /** {@code email} must already be normalized with {@code Validators.email}. */
    Optional<UserAccount> findByEmail(String email);

    boolean existsByEmail(String email);
}

package cl.textilcircular.api.core.repository;

import cl.textilcircular.api.core.model.account.UserAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface UserAccountRepository extends JpaRepository<UserAccount, Long> {

    /**
     * {@code email} must already be normalized with {@code Validators.email}. The query goes through
     * {@code lower(email)} so it matches the unique index {@code uk_user_account_email}.
     */
    @Query("select u from UserAccount u where lower(u.email) = :email")
    Optional<UserAccount> findByEmail(String email);

    @Query("select count(u) > 0 from UserAccount u where lower(u.email) = :email")
    boolean existsByEmail(String email);
}

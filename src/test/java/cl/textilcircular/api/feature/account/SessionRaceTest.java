package cl.textilcircular.api.feature.account;

import cl.textilcircular.api.feature.account.AccountDtos.LoginRequest;
import cl.textilcircular.api.feature.account.AccountDtos.RegisterCustomerRequest;
import cl.textilcircular.api.support.ApiTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Suspending an account ends its sessions with a bulk update that cannot see a token inserted at the
 * same moment. A login that races with the suspension must fail instead of leaving a live session.
 */
@ApiTest
class SessionRaceTest {

    @Autowired AuthService authService;
    @Autowired PlatformTransactionManager transactionManager;
    @Autowired JdbcTemplate jdbc;

    @Test
    void login_racing_with_a_suspension_does_not_leave_a_session() {
        authService.registerCustomer(new RegisterCustomerRequest("cami@demo.cl", "Secreta123", "Cami"));
        TransactionTemplate login = new TransactionTemplate(transactionManager);
        TransactionTemplate suspension = new TransactionTemplate(transactionManager);
        suspension.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);

        assertThatThrownBy(() -> login.executeWithoutResult(status -> {
            authService.login(new LoginRequest("cami@demo.cl", "Secreta123"));
            // Commits while the login is still open, as a request on another thread would.
            suspension.executeWithoutResult(other -> jdbc.update(
                    "UPDATE user_account SET status = 'SUSPENDED', version = version + 1 WHERE email = 'cami@demo.cl'"));
        })).isInstanceOf(OptimisticLockingFailureException.class);

        // Only the token of the registration exists: the racing login left nothing behind.
        assertThat(jdbc.queryForObject("SELECT count(*) FROM refresh_token", Integer.class)).isEqualTo(1);
    }
}

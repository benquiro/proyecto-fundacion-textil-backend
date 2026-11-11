package cl.textilcircular.api.core.web;

import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ProblemDetail;

import java.sql.SQLException;

import static org.assertj.core.api.Assertions.assertThat;

class ApiExceptionHandlerTest {

    final ApiExceptionHandler handler = new ApiExceptionHandler();

    ProblemDetail violationOf(String constraint) {
        return handler.dataIntegrity(new DataIntegrityViolationException("could not execute statement",
                new ConstraintViolationException("duplicate key", new SQLException(), constraint)));
    }

    @Test
    void a_unique_email_or_rut_hit_by_a_race_keeps_its_own_code() {
        assertThat(violationOf("uk_user_account_email").getProperties()).containsEntry("code", "EMAIL_TAKEN");
        assertThat(violationOf("uk_entrepreneur_rut").getProperties()).containsEntry("code", "RUT_TAKEN");
    }

    @Test
    void any_other_constraint_stays_a_generic_conflict() {
        ProblemDetail problem = violationOf("uk_refresh_token_hash");
        assertThat(problem.getStatus()).isEqualTo(409);
        assertThat(problem.getProperties()).containsEntry("code", "CONFLICT");
        assertThat(handler.dataIntegrity(new DataIntegrityViolationException("no cause")).getProperties())
                .containsEntry("code", "CONFLICT");
    }
}

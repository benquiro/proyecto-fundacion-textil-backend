package cl.textilcircular.api.support;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.jdbc.Sql;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Full application with MockMvc against a real PostgreSQL. Each request commits its own
 * transaction, as in production; every account (and what hangs from it) is removed after each test.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@SpringBootTest
@AutoConfigureMockMvc
@Sql(statements = "TRUNCATE user_account CASCADE", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
@Import(TestcontainersConfiguration.class)
public @interface ApiTest {
}

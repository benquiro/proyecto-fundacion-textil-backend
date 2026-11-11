package cl.textilcircular.api.support;

import cl.textilcircular.api.core.config.ClockConfig;
import cl.textilcircular.api.core.config.JpaAuditingConfig;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Repository slice against a real PostgreSQL (Flyway migrations + {@code ddl-auto=validate}).
 * Each test runs in a transaction that is rolled back.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({TestcontainersConfiguration.class, ClockConfig.class, JpaAuditingConfig.class})
public @interface RepositoryTest {
}

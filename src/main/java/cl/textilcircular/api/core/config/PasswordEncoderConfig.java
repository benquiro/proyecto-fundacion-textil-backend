package cl.textilcircular.api.core.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * BCrypt by default; the delegating encoder stores the algorithm id ({@code {bcrypt}...})
 * so hashes can be upgraded later without a migration.
 */
@Configuration(proxyBeanMethods = false)
public class PasswordEncoderConfig {

    @Bean
    PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }
}

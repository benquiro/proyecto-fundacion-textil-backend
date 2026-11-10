package cl.textilcircular.api.core.security;

import cl.textilcircular.api.core.repository.RefreshTokenRepository;
import cl.textilcircular.api.core.web.Problems;
import com.nimbusds.jose.jwk.source.ImmutableSecret;
import com.nimbusds.jose.proc.SecurityContext;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Clock;
import java.util.List;

import static cl.textilcircular.api.core.web.WebConfig.API_BASE_PATH;

/**
 * Stateless API: every protected request carries a bearer access token (HS256) issued by
 * {@link TokenService}. The caller's id is the token subject and its role the {@code role} claim; the
 * token is rejected once its session ({@code sid}) has ended, see {@link SessionValidator}.
 */
@Slf4j
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(SecurityProperties.class)
public class SecurityConfig {

    private static final int MIN_SECRET_BYTES = 32;

    @Bean
    SecurityFilterChain apiSecurity(HttpSecurity http, JwtDecoder jwtDecoder) throws Exception {
        JwtAuthenticationConverter authentication = new JwtAuthenticationConverter();
        authentication.setJwtGrantedAuthoritiesConverter(jwt ->
                List.of(new SimpleGrantedAuthority("ROLE_" + jwt.getClaimAsString(TokenService.ROLE_CLAIM))));

        http.csrf(csrf -> csrf.disable())
                .httpBasic(basic -> basic.disable())
                .formLogin(form -> form.disable())
                .logout(logout -> logout.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.POST, API_BASE_PATH + "/auth/**").permitAll()
                        .requestMatchers(HttpMethod.GET, API_BASE_PATH + "/regions", API_BASE_PATH + "/communes",
                                API_BASE_PATH + "/services", API_BASE_PATH + "/age-ranges",
                                API_BASE_PATH + "/genders").permitAll()
                        .requestMatchers(API_BASE_PATH + "/admin/**").hasRole("ADMINISTRATOR")
                        .requestMatchers("/actuator/health", "/v3/api-docs/**", "/swagger-ui/**",
                                "/swagger-ui.html", "/error").permitAll()
                        .anyRequest().authenticated())
                .oauth2ResourceServer(oauth -> oauth
                        .jwt(jwt -> jwt.decoder(jwtDecoder).jwtAuthenticationConverter(authentication))
                        .authenticationEntryPoint((request, response, e) -> writeProblem(response,
                                HttpServletResponse.SC_UNAUTHORIZED, "Unauthorized", "UNAUTHORIZED",
                                Problems.UNAUTHORIZED_DETAIL))
                        .accessDeniedHandler((request, response, e) -> writeProblem(response,
                                HttpServletResponse.SC_FORBIDDEN, "Forbidden", "FORBIDDEN",
                                Problems.FORBIDDEN_DETAIL)));
        return http.build();
    }

    @Bean
    SecretKey jwtKey(SecurityProperties properties) {
        byte[] secret;
        if (properties.jwtSecret() == null || properties.jwtSecret().isBlank()) {
            log.warn("app.security.jwt-secret is not set: using a random key, sessions end on restart");
            secret = new byte[MIN_SECRET_BYTES];
            new SecureRandom().nextBytes(secret);
        } else {
            secret = properties.jwtSecret().getBytes(StandardCharsets.UTF_8);
            if (secret.length < MIN_SECRET_BYTES) {
                throw new IllegalStateException("app.security.jwt-secret must have at least 32 bytes");
            }
        }
        return new SecretKeySpec(secret, "HmacSHA256");
    }

    @Bean
    JwtEncoder jwtEncoder(SecretKey jwtKey) {
        return new NimbusJwtEncoder(new ImmutableSecret<SecurityContext>(jwtKey));
    }

    @Bean
    JwtDecoder jwtDecoder(SecretKey jwtKey, RefreshTokenRepository refreshTokens, Clock clock) {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withSecretKey(jwtKey).macAlgorithm(MacAlgorithm.HS256).build();
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(JwtValidators.createDefault(),
                new SessionValidator(refreshTokens, clock)));
        return decoder;
    }

    /** Fixed texts only, so the body is built by hand instead of going through a message converter. */
    private static void writeProblem(HttpServletResponse response, int status, String title, String code,
                                     String detail) throws IOException {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write("""
                {"type":"about:blank","title":"%s","status":%d,"code":"%s","detail":"%s"}"""
                .formatted(title, status, code, detail));
    }
}

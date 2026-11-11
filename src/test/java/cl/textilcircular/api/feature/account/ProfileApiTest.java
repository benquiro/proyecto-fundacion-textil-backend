package cl.textilcircular.api.feature.account;

import cl.textilcircular.api.core.model.account.UserAccount;
import cl.textilcircular.api.core.repository.UserAccountRepository;
import cl.textilcircular.api.support.ApiTest;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ApiTest
class ProfileApiTest {

    @Autowired
    MockMvc mvc;
    @Autowired
    UserAccountRepository accounts;
    @Autowired
    PasswordEncoder encoder;
    @Autowired
    JdbcTemplate jdbc;
    @Autowired
    JwtEncoder jwtEncoder;

    ResultActions call(HttpMethod method, String path, String token, String json) throws Exception {
        var builder = request(method, "/api/v1" + path).contentType(MediaType.APPLICATION_JSON);
        if (token != null) {
            builder.header("Authorization", "Bearer " + token);
        }
        return mvc.perform(json == null ? builder : builder.content(json));
    }

    record Tokens(String access, String refresh, String userId) {
    }

    Tokens session(ResultActions result) throws Exception {
        String body = result.andReturn().getResponse().getContentAsString();
        return new Tokens(JsonPath.read(body, "$.accessToken"), JsonPath.read(body, "$.refreshToken"),
                JsonPath.read(body, "$.user.id").toString());
    }

    Tokens signUp(String email) throws Exception {
        return session(call(HttpMethod.POST, "/auth/register/customer", null,
                "{\"email\":\"%s\",\"password\":\"Secreta123\",\"alias\":\"Cami\"}".formatted(email)));
    }

    Tokens login(String email, String password) throws Exception {
        return session(call(HttpMethod.POST, "/auth/login", null,
                "{\"email\":\"%s\",\"password\":\"%s\"}".formatted(email, password)).andExpect(status().isOk()));
    }

    @Test
    void customer_reads_and_edits_the_profile() throws Exception {
        Tokens cami = signUp("cami@demo.cl");
        signUp("otra@demo.cl");

        call(HttpMethod.GET, "/me", cami.access(), null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("cami@demo.cl"))
                .andExpect(jsonPath("$.entrepreneur").doesNotExist());

        call(HttpMethod.PUT, "/me", cami.access(), customerProfile("Nueva@demo.cl", "Camila"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("nueva@demo.cl"))
                .andExpect(jsonPath("$.alias").value("Camila"));

        call(HttpMethod.PUT, "/me", cami.access(), customerProfile("otra@demo.cl", "Camila"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("EMAIL_TAKEN"));
        call(HttpMethod.PUT, "/me", cami.access(), customerProfile("nueva@demo.cl", null))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("alias"));
    }

    static String customerProfile(String email, String alias) {
        return """
                {"email":"%s","alias":%s,"firstName":null,"lastName":null,"ageRangeId":null,"genderId":null}"""
                .formatted(email, alias == null ? "null" : "\"" + alias + "\"");
    }

    Tokens signUpEntrepreneur() throws Exception {
        return session(call(HttpMethod.POST, "/auth/register/entrepreneur", null, """
                {"email":"ana@demo.cl","password":"Secreta123","alias":"Anita","rut":"11111111-1","firstName":"Ana",
                 "lastName":"Pérez","ageRangeId":2,"genderId":1,
                 "business":{"name":"Hilos de Ana","description":"Reparación y transformación de prendas con técnicas de costura a mano.",
                             "photoUrl":null,"communeId":1,"serviceIds":[2]}}""").andExpect(status().isCreated()));
    }

    @Test
    void entrepreneur_profile_keeps_the_rut() throws Exception {
        Tokens ana = signUpEntrepreneur();

        call(HttpMethod.PUT, "/me", ana.access(), """
                {"email":"ana@demo.cl","alias":"Anita","firstName":"Ana María","lastName":"Pérez","ageRangeId":2,"genderId":2}""")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.entrepreneur.rut").value("11111111-1"))
                .andExpect(jsonPath("$.entrepreneur.firstName").value("Ana María"))
                .andExpect(jsonPath("$.entrepreneur.ageRangeId").value(2))
                .andExpect(jsonPath("$.entrepreneur.genderId").value(2));
    }

    @Test
    void put_without_a_property_is_rejected_and_changes_nothing() throws Exception {
        Tokens cami = signUp("cami@demo.cl");

        call(HttpMethod.PUT, "/me", cami.access(), """
                {"email":"nueva@demo.cl","firstName":null,"lastName":null,"ageRangeId":null,"genderId":null}""")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION"))
                .andExpect(jsonPath("$.errors[0].field").value("alias"))
                .andExpect(jsonPath("$.errors[0].message").value("Falta este campo. Envíalo como null si quieres dejarlo vacío"));
        call(HttpMethod.GET, "/me", cami.access(), null)
                .andExpect(jsonPath("$.email").value("cami@demo.cl"))
                .andExpect(jsonPath("$.alias").value("Cami"));
    }

    @Test
    void customer_cannot_send_entrepreneur_data() throws Exception {
        Tokens cami = signUp("cami@demo.cl");

        call(HttpMethod.PUT, "/me", cami.access(), """
                {"email":"cami@demo.cl","alias":"Otra","firstName":"Ana","lastName":null,"ageRangeId":null,"genderId":null}""")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION"))
                .andExpect(jsonPath("$.errors[0].field").value("firstName"))
                .andExpect(jsonPath("$.errors[0].message").value("Solo las personas emprendedoras tienen este dato"));
        call(HttpMethod.GET, "/me", cami.access(), null).andExpect(jsonPath("$.alias").value("Cami"));
    }

    @Test
    void entrepreneur_put_requires_the_demographics() throws Exception {
        Tokens ana = signUpEntrepreneur();

        call(HttpMethod.PUT, "/me", ana.access(), """
                {"email":"ana@demo.cl","alias":"Otra","firstName":"Ana","lastName":"Pérez","ageRangeId":null,"genderId":1}""")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION"))
                .andExpect(jsonPath("$.errors[0].field").value("ageRangeId"))
                .andExpect(jsonPath("$.errors[0].message").value("Elige tu rango de edad"));
        call(HttpMethod.GET, "/me", ana.access(), null)
                .andExpect(jsonPath("$.alias").value("Anita"))
                .andExpect(jsonPath("$.entrepreneur.ageRangeId").value(2));
    }

    @Test
    void entrepreneur_can_clear_the_alias() throws Exception {
        Tokens ana = signUpEntrepreneur();

        call(HttpMethod.PUT, "/me", ana.access(), """
                {"email":"ana@demo.cl","alias":null,"firstName":"Ana","lastName":"Pérez","ageRangeId":2,"genderId":1}""")
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("\"alias\":null")))
                .andExpect(jsonPath("$.alias").value(nullValue()));
    }

    @Test
    void patch_me_is_gone() throws Exception {
        Tokens cami = signUp("cami@demo.cl");

        call(HttpMethod.PATCH, "/me", cami.access(), customerProfile("cami@demo.cl", "Camila"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.code").value("METHOD_NOT_ALLOWED"));
    }

    @Test
    void password_change_ends_the_other_sessions_only() throws Exception {
        Tokens first = signUp("cami@demo.cl");
        Tokens second = login("cami@demo.cl", "Secreta123");

        call(HttpMethod.PUT, "/me/password", first.access(),
                "{\"currentPassword\":\"Mala1234\",\"newPassword\":\"Nueva1234\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("currentPassword"));
        call(HttpMethod.PUT, "/me/password", first.access(),
                "{\"currentPassword\":\"Secreta123\",\"newPassword\":\"Nueva1234\"}")
                .andExpect(status().isNoContent());

        call(HttpMethod.POST, "/auth/refresh", null, "{\"refreshToken\":\"%s\"}".formatted(second.refresh()))
                .andExpect(status().isUnauthorized());
        call(HttpMethod.POST, "/auth/refresh", null, "{\"refreshToken\":\"%s\"}".formatted(first.refresh()))
                .andExpect(status().isOk());
        login("cami@demo.cl", "Nueva1234");
    }

    @Test
    void a_request_without_a_token_gets_the_common_error_body_and_a_bearer_challenge() throws Exception {
        call(HttpMethod.GET, "/me", null, null)
                .andExpect(status().isUnauthorized())
                .andExpect(header().string("WWW-Authenticate", "Bearer"))
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
                .andExpect(jsonPath("$.detail").value("Tu sesión expiró. Inicia sesión de nuevo"))
                .andExpect(jsonPath("$.instance").value("/api/v1/me"));
    }

    String refresh(String refreshToken) throws Exception {
        return JsonPath.read(call(HttpMethod.POST, "/auth/refresh", null,
                        "{\"refreshToken\":\"%s\"}".formatted(refreshToken))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString(), "$.refreshToken");
    }

    @Test
    void access_token_dies_with_its_session() throws Exception {
        Tokens cami = signUp("cami@demo.cl");
        refresh(cami.refresh());
        call(HttpMethod.GET, "/me", cami.access(), null)
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));

        Tokens again = login("cami@demo.cl", "Secreta123");
        call(HttpMethod.POST, "/auth/logout", null, "{\"refreshToken\":\"%s\"}".formatted(again.refresh()))
                .andExpect(status().isNoContent());
        call(HttpMethod.GET, "/me", again.access(), null).andExpect(status().isUnauthorized());
    }

    @Test
    void retrying_a_token_right_after_its_rotation_is_refused_but_keeps_the_sessions() throws Exception {
        Tokens phone = signUp("cami@demo.cl");
        Tokens tablet = login("cami@demo.cl", "Secreta123");
        String rotated = refresh(phone.refresh());

        call(HttpMethod.POST, "/auth/refresh", null, "{\"refreshToken\":\"%s\"}".formatted(phone.refresh()))
                .andExpect(status().isUnauthorized());
        call(HttpMethod.POST, "/auth/refresh", null, "{\"refreshToken\":\"%s\"}".formatted(rotated))
                .andExpect(status().isOk());
        call(HttpMethod.GET, "/me", tablet.access(), null).andExpect(status().isOk());
    }

    @Test
    void replaying_a_rotated_refresh_token_later_ends_every_session() throws Exception {
        Tokens phone = signUp("cami@demo.cl");
        Tokens tablet = login("cami@demo.cl", "Secreta123");
        String rotated = refresh(phone.refresh());
        jdbc.update("UPDATE refresh_token SET rotated_at = rotated_at - interval '1 minute' WHERE rotated_at IS NOT NULL");

        call(HttpMethod.POST, "/auth/refresh", null, "{\"refreshToken\":\"%s\"}".formatted(phone.refresh()))
                .andExpect(status().isUnauthorized());
        call(HttpMethod.POST, "/auth/refresh", null, "{\"refreshToken\":\"%s\"}".formatted(rotated))
                .andExpect(status().isUnauthorized());
        call(HttpMethod.GET, "/me", tablet.access(), null).andExpect(status().isUnauthorized());
        login("cami@demo.cl", "Secreta123");
    }

    static final String ENTREPRENEUR = """
            {"email":"ana@demo.cl","password":"Secreta123","rut":"11111111-1","firstName":"Ana",
             "lastName":"Pérez","ageRangeId":2,"genderId":1,
             "business":{"name":"Hilos de Ana","description":"Reparación y transformación de prendas con técnicas de costura a mano.",
                         "photoUrl":null,"communeId":1,"serviceIds":[2]}}""";

    @Test
    void deleting_an_entrepreneur_ends_the_session_and_frees_email_and_rut() throws Exception {
        Tokens ana = session(call(HttpMethod.POST, "/auth/register/entrepreneur", null, ENTREPRENEUR));

        call(HttpMethod.DELETE, "/me", ana.access(), null).andExpect(status().isNoContent());

        call(HttpMethod.GET, "/me", ana.access(), null).andExpect(status().isUnauthorized());
        call(HttpMethod.POST, "/auth/login", null, "{\"email\":\"ana@demo.cl\",\"password\":\"Secreta123\"}")
                .andExpect(status().isUnauthorized());
        call(HttpMethod.POST, "/auth/register/entrepreneur", null, ENTREPRENEUR).andExpect(status().isCreated());
    }

    @Test
    void an_access_token_with_a_malformed_subject_or_session_is_a_plain_401() throws Exception {
        Instant now = Instant.now();
        for (JwtClaimsSet.Builder claims : new JwtClaimsSet.Builder[]{
                JwtClaimsSet.builder().subject("not-a-number").claim("sid", 1L),
                JwtClaimsSet.builder().subject("1").claim("sid", "x")}) {
            String token = jwtEncoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(),
                    claims.claim("role", "CUSTOMER").issuedAt(now).expiresAt(now.plus(5, ChronoUnit.MINUTES)).build()))
                    .getTokenValue();
            call(HttpMethod.GET, "/me", token, null)
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
        }
    }

    @Test
    void unknown_path_is_a_plain_not_found() throws Exception {
        Tokens cami = signUp("cami@demo.cl");
        call(HttpMethod.GET, "/ADMIN/users", cami.access(), null)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"))
                .andExpect(jsonPath("$.detail").value(org.hamcrest.Matchers.not(
                        org.hamcrest.Matchers.containsString("ADMIN"))));
    }

    @Test
    void deactivation_ends_sessions_and_login_brings_the_account_back() throws Exception {
        Tokens cami = signUp("cami@demo.cl");

        call(HttpMethod.POST, "/me/deactivation", cami.access(), null).andExpect(status().isNoContent());
        call(HttpMethod.GET, "/me", cami.access(), null)
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
        call(HttpMethod.POST, "/auth/refresh", null, "{\"refreshToken\":\"%s\"}".formatted(cami.refresh()))
                .andExpect(status().isUnauthorized());

        Tokens back = login("cami@demo.cl", "Secreta123");
        call(HttpMethod.DELETE, "/me", back.access(), null).andExpect(status().isNoContent());
        call(HttpMethod.POST, "/auth/login", null, "{\"email\":\"cami@demo.cl\",\"password\":\"Secreta123\"}")
                .andExpect(status().isUnauthorized());
    }

    @Test
    void administrator_suspends_and_reactivates_users() throws Exception {
        accounts.save(UserAccount.registerAdministrator("admin@demo.cl", "Admin1234", null, encoder, Instant.now()));
        Tokens admin = login("admin@demo.cl", "Admin1234");
        Tokens cami = signUp("cami@demo.cl");

        call(HttpMethod.GET, "/admin/users", cami.access(), null)
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
        call(HttpMethod.GET, "/admin/users?size=1", admin.access(), null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.last").value(false));

        call(HttpMethod.PUT, "/admin/users/" + cami.userId() + "/suspension", admin.access(), null)
                .andExpect(status().isNoContent());
        call(HttpMethod.POST, "/auth/login", null, "{\"email\":\"cami@demo.cl\",\"password\":\"Secreta123\"}")
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.accountStatus").value("SUSPENDED"))
                .andExpect(jsonPath("$.detail").value("Tu cuenta está suspendida"));
        call(HttpMethod.DELETE, "/me", cami.access(), null).andExpect(status().isUnauthorized());

        call(HttpMethod.DELETE, "/admin/users/" + cami.userId() + "/suspension", admin.access(), null)
                .andExpect(status().isNoContent());
        login("cami@demo.cl", "Secreta123");
        call(HttpMethod.PUT, "/admin/users/" + admin.userId() + "/suspension", admin.access(), null)
                .andExpect(status().isForbidden());
    }
}

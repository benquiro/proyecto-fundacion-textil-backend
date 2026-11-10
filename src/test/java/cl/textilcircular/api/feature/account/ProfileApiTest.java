package cl.textilcircular.api.feature.account;

import cl.textilcircular.api.core.model.account.UserAccount;
import cl.textilcircular.api.core.repository.UserAccountRepository;
import cl.textilcircular.api.support.ApiTest;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.time.Instant;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;
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

        call(HttpMethod.PATCH, "/me", cami.access(), "{\"email\":\"Nueva@demo.cl\",\"alias\":\"Camila\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("nueva@demo.cl"))
                .andExpect(jsonPath("$.alias").value("Camila"));

        call(HttpMethod.PATCH, "/me", cami.access(), "{\"email\":\"otra@demo.cl\",\"alias\":\"Camila\"}")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("EMAIL_TAKEN"));
        call(HttpMethod.PATCH, "/me", cami.access(), "{\"email\":\"nueva@demo.cl\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("alias"));
    }

    @Test
    void entrepreneur_profile_keeps_the_rut() throws Exception {
        Tokens ana = session(call(HttpMethod.POST, "/auth/register/entrepreneur", null, """
                {"email":"ana@demo.cl","password":"Secreta123","rut":"11111111-1","firstName":"Ana",
                 "lastName":"Pérez","ageRangeId":2,"genderId":1,
                 "business":{"name":"Hilos de Ana","description":"Reparación y transformación de prendas con técnicas de costura a mano.",
                             "communeId":1,"serviceIds":[2]}}"""));

        call(HttpMethod.PATCH, "/me", ana.access(), """
                {"email":"ana@demo.cl","firstName":"Ana María","lastName":"Pérez","genderId":2}""")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.entrepreneur.rut").value("11111111-1"))
                .andExpect(jsonPath("$.entrepreneur.firstName").value("Ana María"))
                .andExpect(jsonPath("$.entrepreneur.ageRangeId").value(2))
                .andExpect(jsonPath("$.entrepreneur.genderId").value(2));
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
    void deactivation_ends_sessions_and_login_brings_the_account_back() throws Exception {
        Tokens cami = signUp("cami@demo.cl");

        call(HttpMethod.POST, "/me/deactivation", cami.access(), null).andExpect(status().isNoContent());
        call(HttpMethod.GET, "/me", cami.access(), null)
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCOUNT_NOT_ACTIVE"))
                .andExpect(jsonPath("$.accountStatus").value("DEACTIVATED"));
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
        call(HttpMethod.DELETE, "/me", cami.access(), null)
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.accountStatus").value("SUSPENDED"));

        call(HttpMethod.DELETE, "/admin/users/" + cami.userId() + "/suspension", admin.access(), null)
                .andExpect(status().isNoContent());
        login("cami@demo.cl", "Secreta123");
        call(HttpMethod.PUT, "/admin/users/" + admin.userId() + "/suspension", admin.access(), null)
                .andExpect(status().isForbidden());
    }
}

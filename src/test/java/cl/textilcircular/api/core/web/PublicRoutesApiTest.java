package cl.textilcircular.api.core.web;

import cl.textilcircular.api.core.model.account.UserAccount;
import cl.textilcircular.api.core.repository.UserAccountRepository;
import cl.textilcircular.api.support.ApiTest;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.time.Instant;
import java.util.List;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ApiTest
class PublicRoutesApiTest {

    @Autowired
    MockMvc mvc;

    /** Whatever the handler answers (it may not exist yet), the security filter let the guest through. */
    static final ResultMatcher NOT_REJECTED = result ->
            assertThat(result.getResponse().getStatus()).as("guest rejected").isNotIn(401, 403);

    @Test
    void guests_reach_public_routes() throws Exception {
        mvc.perform(get("/api/v1/contents")).andExpect(NOT_REJECTED);
        mvc.perform(get("/api/v1/contents/1")).andExpect(NOT_REJECTED);
        mvc.perform(get("/api/v1/businesses")).andExpect(NOT_REJECTED);
        mvc.perform(get("/api/v1/businesses/1")).andExpect(NOT_REJECTED);
        mvc.perform(get("/api/v1/products/1")).andExpect(NOT_REJECTED);
        mvc.perform(post("/api/v1/businesses/1/contact-clicks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(NOT_REJECTED);
    }

    @Autowired
    UserAccountRepository accounts;
    @Autowired
    PasswordEncoder encoder;

    String login(String email) throws Exception {
        String body = mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"%s\",\"password\":\"Demo1234\"}".formatted(email)))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.accessToken");
    }

    @Test
    void only_administrators_write_contents() throws Exception {
        accounts.save(UserAccount.registerAdministrator("admin@demo.cl", "Demo1234", null, encoder, Instant.now()));
        accounts.save(UserAccount.registerCustomer("cami@demo.cl", "Demo1234", "Cami", encoder, Instant.now()));
        String admin = "Bearer " + login("admin@demo.cl");
        String customer = "Bearer " + login("cami@demo.cl");
        // A builder keeps the headers added to it, so each request starts from a fresh one.
        List<Supplier<MockHttpServletRequestBuilder>> writes = List.of(
                () -> post("/api/v1/contents").contentType(MediaType.APPLICATION_JSON).content("{}"),
                () -> put("/api/v1/contents/1").contentType(MediaType.APPLICATION_JSON).content("{}"),
                () -> delete("/api/v1/contents/1"));

        for (Supplier<MockHttpServletRequestBuilder> write : writes) {
            mvc.perform(write.get()).andExpect(status().isUnauthorized());
            mvc.perform(write.get().header("Authorization", customer))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.code").value("FORBIDDEN"));
            mvc.perform(write.get().header("Authorization", admin)).andExpect(NOT_REJECTED);
        }
    }

    @Test
    void invalid_token_on_public_route_is_rejected() throws Exception {
        mvc.perform(get("/api/v1/businesses").header("Authorization", "Bearer not-a-token"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    void private_routes_still_require_a_session() throws Exception {
        mvc.perform(get("/api/v1/me/business")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/v1/conversations")).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/v1/media")).andExpect(status().isUnauthorized());
    }
}

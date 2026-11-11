package cl.textilcircular.api.core.web;

import cl.textilcircular.api.support.ApiTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultMatcher;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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

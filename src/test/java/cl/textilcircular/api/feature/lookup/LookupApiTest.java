package cl.textilcircular.api.feature.lookup;

import cl.textilcircular.api.support.ApiTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ApiTest
class LookupApiTest {

    @Autowired
    MockMvc mvc;

    @Test
    void reference_lists_are_public() throws Exception {
        mvc.perform(get("/api/v1/regions")).andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(16)));
        mvc.perform(get("/api/v1/communes")).andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(346)))
                .andExpect(jsonPath("$[0].regionId").isNumber());
        mvc.perform(get("/api/v1/services")).andExpect(status().isOk()).andExpect(jsonPath("$[0].name").isString());
        mvc.perform(get("/api/v1/age-ranges")).andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(7)))
                .andExpect(jsonPath("$[0].description").isString());
        mvc.perform(get("/api/v1/genders")).andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(4)));
    }

    @Test
    void protected_routes_answer_with_the_contract_problem() throws Exception {
        mvc.perform(get("/api/v1/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
                .andExpect(jsonPath("$.detail").value("Tu sesión expiró. Inicia sesión de nuevo"));
        mvc.perform(get("/api/v1/me").header("Authorization", "Bearer not-a-token"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }
}

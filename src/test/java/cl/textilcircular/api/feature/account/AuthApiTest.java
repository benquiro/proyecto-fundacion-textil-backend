package cl.textilcircular.api.feature.account;

import cl.textilcircular.api.core.model.business.BusinessStatus;
import cl.textilcircular.api.core.repository.BusinessRepository;
import cl.textilcircular.api.support.ApiTest;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ApiTest
class AuthApiTest {

    static final String CUSTOMER = """
            {"email":" Cami@Demo.cl ","password":"Secreta123","alias":"Cami"}""";

    @Autowired
    MockMvc mvc;
    @Autowired
    BusinessRepository businesses;

    ResultActions post(String path, String json) throws Exception {
        return mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/v1" + path)
                .contentType(MediaType.APPLICATION_JSON).content(json));
    }

    static String read(ResultActions result, String path) throws Exception {
        return JsonPath.read(result.andReturn().getResponse().getContentAsString(), path).toString();
    }

    @Test
    void customer_signs_up_logs_in_and_rotates_the_session() throws Exception {
        post("/auth/register/customer", CUSTOMER)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accessToken").isString())
                .andExpect(jsonPath("$.expiresIn").value(900))
                .andExpect(jsonPath("$.user.email").value("cami@demo.cl"))
                .andExpect(jsonPath("$.user.role").value("CUSTOMER"))
                .andExpect(jsonPath("$.user.status").value("ACTIVE"));

        post("/auth/register/customer", CUSTOMER)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("EMAIL_TAKEN"))
                .andExpect(jsonPath("$.detail").value("Ese correo ya tiene una cuenta"));

        post("/auth/login", """
                {"email":"cami@demo.cl","password":"Mala1234"}""")
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"))
                .andExpect(jsonPath("$.detail").value("Correo o contraseña incorrectos"));
        post("/auth/login", """
                {"email":"nadie@demo.cl","password":"Mala1234"}""")
                .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"));

        String refresh = read(post("/auth/login", """
                {"email":"CAMI@demo.cl","password":"Secreta123"}""").andExpect(status().isOk()), "$.refreshToken");

        String rotated = read(post("/auth/refresh", "{\"refreshToken\":\"" + refresh + "\"}")
                .andExpect(status().isOk()), "$.refreshToken");
        assertThat(rotated).isNotEqualTo(refresh);
        post("/auth/refresh", "{\"refreshToken\":\"" + refresh + "\"}")
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));

        post("/auth/logout", "{\"refreshToken\":\"" + rotated + "\"}").andExpect(status().isNoContent());
        post("/auth/logout", "{\"refreshToken\":\"unknown\"}").andExpect(status().isNoContent());
        post("/auth/refresh", "{\"refreshToken\":\"" + rotated + "\"}").andExpect(status().isUnauthorized());
    }

    @Test
    void invalid_sign_up_names_the_field() throws Exception {
        post("/auth/register/customer", """
                {"email":"cami@demo.cl","password":"corta","alias":"Cami"}""")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION"))
                .andExpect(jsonPath("$.errors[0].field").value("password"));
        post("/auth/register/customer", "not json")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("MALFORMED_REQUEST"));
    }

    @Test
    void entrepreneur_sign_up_creates_a_pending_business() throws Exception {
        String body = """
                {"email":"ana@demo.cl","password":"Secreta123","rut":"11.111.111-1","firstName":"Ana",
                 "lastName":"Pérez","ageRangeId":2,"genderId":1,
                 "business":{"name":"Hilos de Ana","description":"Reparación y transformación de prendas con técnicas de costura a mano.",
                             "communeId":1,"serviceIds":[%s]}}""";

        post("/auth/register/entrepreneur", body.formatted("1,1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("business.serviceIds"));

        String id = read(post("/auth/register/entrepreneur", body.formatted("2,1"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.user.role").value("ENTREPRENEUR")), "$.user.id");
        assertThat(businesses.findByOwnerId(Long.valueOf(id)))
                .hasValueSatisfying(b -> assertThat(b.getStatus()).isEqualTo(BusinessStatus.PENDING));

        post("/auth/register/entrepreneur", body.formatted("2").replace("ana@demo.cl", "otra@demo.cl"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("RUT_TAKEN"));
    }
}

package cl.textilcircular.api.core.web;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.mock.http.MockHttpInputMessage;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.ServletWebRequest;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchException;

/** A property missing inside a list element is named like the contract says: no index. */
class MissingPropertyTest {

    record Item(@JsonProperty(required = true) String name) {
    }

    record Order(@JsonProperty(required = true) List<Item> items) {
    }

    @Test
    void a_missing_property_inside_a_list_is_reported_without_the_index() {
        Exception parsing = catchException(() -> JsonMapper.builder().build().readValue("{\"items\":[{}]}", Order.class));
        var failure = new HttpMessageNotReadableException("unreadable", parsing, new MockHttpInputMessage(new byte[0]));

        var response = new ApiExceptionHandler().handleHttpMessageNotReadable(failure, new HttpHeaders(),
                HttpStatus.BAD_REQUEST, new ServletWebRequest(new MockHttpServletRequest()));

        ProblemDetail problem = (ProblemDetail) response.getBody();
        assertThat(problem.getProperties()).containsEntry("code", "VALIDATION")
                .containsEntry("errors", List.of(Map.of("field", "items.name", "message", Problems.MISSING_PROPERTY_MESSAGE)));
    }
}

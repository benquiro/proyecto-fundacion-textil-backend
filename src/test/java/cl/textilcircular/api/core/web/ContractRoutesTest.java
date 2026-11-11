package cl.textilcircular.api.core.web;

import cl.textilcircular.api.support.ApiTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.MethodParameter;
import org.springframework.core.ResolvableType;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;
import org.yaml.snakeyaml.Yaml;

import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Every handler must be an operation of {@code docs/api/openapi.yaml}: same method and path, the declared
 * success status, and a body exactly when the contract's 2xx response has one (the app crashes on a 204
 * where it expects a body). Field shapes stay covered by each endpoint's own tests.
 */
@ApiTest
class ContractRoutesTest {

    private static final Path CONTRACT = Path.of("docs/api/openapi.yaml");

    @Autowired
    @Qualifier("requestMappingHandlerMapping")
    RequestMappingHandlerMapping mapping;

    /** {@code components.responses}, for the responses written as {@code $ref}. */
    private Map<?, ?> sharedResponses = Map.of();

    @Test
    @SuppressWarnings("unchecked")
    void every_handler_matches_its_contract_operation() throws Exception {
        Map<String, Object> contract;
        try (Reader reader = Files.newBufferedReader(CONTRACT)) {
            contract = new Yaml().load(reader);
        }
        if (contract.get("components") instanceof Map<?, ?> components
                && components.get("responses") instanceof Map<?, ?> responses) {
            sharedResponses = responses;
        }
        Map<String, Map<String, Object>> paths = new HashMap<>();
        ((Map<String, Map<String, Object>>) contract.get("paths"))
                .forEach((path, operations) -> paths.put(normalize(path), operations));
        List<String> mismatches = new ArrayList<>();

        mapping.getHandlerMethods().forEach((info, method) -> {
            if (method.getBeanType().getPackageName().startsWith("cl.textilcircular.api.feature")) {
                check(info, method, paths, mismatches);
            }
        });
        paths.forEach((path, operations) -> operations.forEach((verb, operation) ->
                responses(operation).forEach((code, response) -> {
                    if (code.equals("204") && hasContent(response)) {
                        mismatches.add(verb.toUpperCase() + " " + path + ": a 204 response cannot have content");
                    }
                })));

        assertThat(mismatches).isEmpty();
    }

    private void check(RequestMappingInfo info, HandlerMethod method, Map<String, Map<String, Object>> paths,
                              List<String> mismatches) {
        Set<RequestMethod> verbs = info.getMethodsCondition().getMethods();
        if (verbs.isEmpty()) {
            mismatches.add(method.getShortLogMessage() + " has no explicit HTTP method");
            return;
        }
        for (String pattern : info.getPatternValues()) {
            if (!pattern.startsWith(WebConfig.API_BASE_PATH)) {
                mismatches.add(pattern + " is outside " + WebConfig.API_BASE_PATH);
                continue;
            }
            String path = normalize(pattern.substring(WebConfig.API_BASE_PATH.length()));
            for (RequestMethod verb : verbs) {
                String route = verb + " " + path;
                Map<String, Object> operations = paths.get(path);
                Object operation = operations == null ? null : operations.get(verb.name().toLowerCase());
                if (operation == null) {
                    mismatches.add(route + " is not in the contract");
                    continue;
                }
                compare(route, method, responses(operation), mismatches);
            }
        }
    }

    private void compare(String route, HandlerMethod method, Map<String, Object> responses,
                                List<String> mismatches) {
        Set<String> successCodes = new TreeSet<>();
        boolean contractHasBody = false;
        for (Map.Entry<String, Object> response : responses.entrySet()) {
            if (response.getKey().startsWith("2")) {
                successCodes.add(response.getKey());
                contractHasBody |= hasContent(response.getValue());
            }
        }

        MethodParameter returnType = method.getReturnType();
        boolean dynamicStatus = ResponseEntity.class.isAssignableFrom(returnType.getParameterType());
        Class<?> bodyType = dynamicStatus
                ? ResolvableType.forMethodParameter(returnType).getGeneric(0).resolve(Object.class)
                : returnType.getParameterType();
        boolean handlerHasBody = bodyType != void.class && bodyType != Void.class;
        if (handlerHasBody != contractHasBody) {
            mismatches.add(route + ": handler body=" + handlerHasBody + ", contract body=" + contractHasBody);
        }

        if (!dynamicStatus) {
            ResponseStatus annotation = AnnotatedElementUtils.findMergedAnnotation(method.getMethod(), ResponseStatus.class);
            String status = String.valueOf(annotation == null ? 200 : annotation.code().value());
            if (!successCodes.equals(Set.of(status))) {
                mismatches.add(route + ": handler " + status + ", contract " + successCodes);
            }
        }
    }

    /** Handlers and contract name path variables differently ({id} vs {conversationId}). */
    private static String normalize(String path) {
        return path.replaceAll("\\{[^}]+}", "{}");
    }

    private boolean hasContent(Object response) {
        if (response instanceof Map<?, ?> r && r.get("$ref") instanceof String ref) {
            response = sharedResponses.get(ref.substring(ref.lastIndexOf('/') + 1));
        }
        return response instanceof Map<?, ?> r && r.containsKey("content");
    }

    /** Status code (as text: YAML may parse an unquoted 200 as a number) to response. */
    private static Map<String, Object> responses(Object operation) {
        Map<String, Object> byCode = new HashMap<>();
        if (operation instanceof Map<?, ?> op && op.get("responses") instanceof Map<?, ?> responses) {
            responses.forEach((code, response) -> byCode.put(String.valueOf(code), response));
        }
        return byCode;
    }
}

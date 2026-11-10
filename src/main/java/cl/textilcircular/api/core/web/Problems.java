package cl.textilcircular.api.core.web;

import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;

import java.util.List;
import java.util.Map;

/**
 * RFC 9457 bodies with the contract's extensions: {@code code} (stable, machine readable) and
 * {@code errors} (one entry per invalid field). {@code detail} is Spanish text the app shows as is.
 */
public final class Problems {

    public static final String VALIDATION_DETAIL = "Revisa los datos ingresados";
    public static final String UNAUTHORIZED_DETAIL = "Tu sesión expiró. Inicia sesión de nuevo";
    public static final String FORBIDDEN_DETAIL = "No tienes permiso para hacer esto";

    private Problems() {
    }

    public static ProblemDetail of(HttpStatusCode status, String code, String detail) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setProperty("code", code);
        return problem;
    }

    public static ProblemDetail validation(String field, String message) {
        ProblemDetail problem = of(HttpStatus.BAD_REQUEST, "VALIDATION", VALIDATION_DETAIL);
        problem.setProperty("errors", List.of(Map.of("field", field, "message", message)));
        return problem;
    }

    /** Fallback code when a status has no specific one. */
    public static String codeFor(HttpStatusCode status) {
        return switch (status.value()) {
            case 400 -> "BAD_REQUEST";
            case 401 -> "UNAUTHORIZED";
            case 403 -> "FORBIDDEN";
            case 404 -> "NOT_FOUND";
            case 405 -> "METHOD_NOT_ALLOWED";
            case 409 -> "CONFLICT";
            case 415 -> "UNSUPPORTED_MEDIA_TYPE";
            default -> status.is5xxServerError() ? "INTERNAL_ERROR" : "ERROR";
        };
    }
}

package cl.textilcircular.api.core.web;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/** Documentation-only shape of every error body (RFC 9457 problem with the contract's extensions). */
@Schema(name = "ApiProblem", description = "Error body shared by every endpoint")
public record ApiProblem(
        @Schema(description = "HTTP status", example = "400") int status,
        @Schema(description = "Stable machine-readable identifier", example = "VALIDATION") String code,
        @Schema(description = "Spanish text the app shows as is", example = "Revisa los datos ingresados")
        String detail,
        @Schema(description = "HTTP reason phrase", example = "Bad Request") String title,
        @Schema(description = "Request path", example = "/api/v1/auth/login") String instance,
        @Schema(description = "One entry per invalid field; only present for `VALIDATION`") List<FieldError> errors) {

    @Schema(name = "ApiProblemFieldError")
    public record FieldError(
            @Schema(description = "Field name; nested fields use a dot", example = "email") String field,
            @Schema(description = "Spanish message for that field", example = "Ingresa un correo válido")
            String message) {
    }
}

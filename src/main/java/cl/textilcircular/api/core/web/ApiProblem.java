package cl.textilcircular.api.core.web;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/** Documentation-only shape of every error body (RFC 9457 problem with the contract's extensions). */
@Schema(name = "ApiProblem", description = "Cuerpo de error compartido por todos los endpoints")
public record ApiProblem(
        @Schema(description = "Estado HTTP", example = "400") int status,
        @Schema(description = "Identificador estable, legible por máquina", example = "VALIDATION") String code,
        @Schema(description = "Texto en español que la app muestra tal cual", example = "Revisa los datos ingresados")
        String detail,
        @Schema(description = "Descripción del estado HTTP", example = "Bad Request") String title,
        @Schema(description = "Ruta de la petición", example = "/api/v1/auth/login") String instance,
        @Schema(description = "Una entrada por campo inválido; solo aparece con `VALIDATION`") List<FieldError> errors) {

    @Schema(name = "ApiProblemFieldError")
    public record FieldError(
            @Schema(description = "Nombre del campo; los anidados usan punto", example = "email") String field,
            @Schema(description = "Mensaje en español para ese campo", example = "Ingresa un correo válido")
            String message) {
    }
}

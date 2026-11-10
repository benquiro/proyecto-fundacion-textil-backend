package cl.textilcircular.api.core.config;

import cl.textilcircular.api.core.web.ApiProblem;
import io.swagger.v3.core.converter.ModelConverters;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.oas.models.tags.Tag;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * API documentation: the bearer scheme (Swagger UI's Authorize button), the shared error body and the
 * error responses every operation can return, so controllers only document what is specific to them.
 */
@Configuration(proxyBeanMethods = false)
public class OpenApiConfig {

    public static final String BEARER = "bearerAuth";
    private static final String PROBLEM = "ApiProblem";

    @Bean
    OpenAPI openApi() {
        var problem = ModelConverters.getInstance().readAllAsResolvedSchema(ApiProblem.class);
        return new OpenAPI()
                .info(new Info()
                        .title("Textil Circular API")
                        .version("v1")
                        .description("""
                                Backend de la app móvil Textil Circular.

                                **Cómo probar los endpoints protegidos:** llama a `POST /auth/login`, copia el \
                                `accessToken` de la respuesta, presiona **Authorize** y pégalo (sin el prefijo \
                                `Bearer `). Los tokens de acceso duran 15 minutos; renuévalos con `POST /auth/refresh`.

                                Todos los errores usan el mismo cuerpo (`ApiProblem`): `detail` es el texto que la app \
                                muestra tal cual y `code` es el identificador estable para decidir qué hacer."""))
                .addTagsItem(new Tag().name("Auth").description("Registro, inicio de sesión y renovación de sesión. Públicos."))
                .addTagsItem(new Tag().name("Profile").description("La cuenta del usuario con sesión iniciada."))
                .addTagsItem(new Tag().name("Admin").description("Moderación de usuarios. Solo rol ADMINISTRATOR."))
                .addTagsItem(new Tag().name("Lookup").description("Listas de referencia que la app guarda en caché. Públicas."))
                .addServersItem(new Server().url("/").description("Este servidor"))
                .components(new Components()
                        .addSecuritySchemes(BEARER, new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("Token de acceso que entregan el login, el registro y la renovación de sesión."))
                        .schemas(problem.referencedSchemas))
                // Protected by default; public operations opt out with an empty @SecurityRequirements.
                .addSecurityItem(new SecurityRequirement().addList(BEARER));
    }

    /** Adds the error responses that apply to every operation unless the controller already documented them. */
    @Bean
    OperationCustomizer standardErrors() {
        return (Operation operation, org.springframework.web.method.HandlerMethod handler) -> {
            boolean secured = operation.getSecurity() == null || !operation.getSecurity().isEmpty();
            boolean hasInput = operation.getRequestBody() != null
                    || (operation.getParameters() != null && !operation.getParameters().isEmpty());
            if (hasInput) {
                add(operation, "400", "Datos inválidos (`VALIDATION`, `MALFORMED_REQUEST`). `errors` detalla cada campo.");
            }
            if (secured) {
                add(operation, "401", "Token de acceso ausente, vencido o revocado (`UNAUTHORIZED`).");
            }
            add(operation, "500", "Falla inesperada (`INTERNAL_ERROR`).");
            return operation;
        };
    }

    private static void add(Operation operation, String status, String description) {
        if (operation.getResponses() != null && operation.getResponses().containsKey(status)) {
            return;
        }
        MediaType body = new MediaType().schema(new Schema<>().$ref("#/components/schemas/" + PROBLEM));
        operation.getResponses().addApiResponse(status, new ApiResponse().description(description)
                .content(new Content().addMediaType("application/problem+json", body)));
    }
}

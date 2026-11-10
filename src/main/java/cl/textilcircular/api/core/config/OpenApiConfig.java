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
        Schema<?> problem = ModelConverters.getInstance().readAllAsResolvedSchema(ApiProblem.class).schema;
        return new OpenAPI()
                .info(new Info()
                        .title("Textil Circular API")
                        .version("v1")
                        .description("""
                                Backend of the Textil Circular mobile app.

                                **How to try the protected endpoints:** call `POST /auth/login`, copy the \
                                `accessToken` from the response, press **Authorize** and paste it (without the \
                                `Bearer ` prefix). Access tokens last 15 minutes; renew them with `POST /auth/refresh`.

                                Every error uses the same body (`ApiProblem`): `detail` is the Spanish text the app \
                                shows and `code` is the stable identifier to branch on."""))
                .addTagsItem(new Tag().name("Auth").description("Sign-up, login and refresh-token lifecycle. Public."))
                .addTagsItem(new Tag().name("Profile").description("The signed-in user's own account."))
                .addTagsItem(new Tag().name("Admin").description("User moderation. ADMINISTRATOR role only."))
                .addTagsItem(new Tag().name("Lookup").description("Reference lists the app caches. Public."))
                .addServersItem(new Server().url("/").description("This server"))
                .components(new Components()
                        .addSecuritySchemes(BEARER, new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("Access token returned by login, register or refresh."))
                        .addSchemas(PROBLEM, problem))
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
                add(operation, "400", "Invalid data (`VALIDATION`, `MALFORMED_REQUEST`). `errors` lists each field.");
            }
            if (secured) {
                add(operation, "401", "Missing, expired or revoked access token (`UNAUTHORIZED`).");
            }
            add(operation, "500", "Unexpected failure (`INTERNAL_ERROR`).");
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

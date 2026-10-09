package com.escapa.backend.infrastructure.config;

import com.escapa.backend.adapters.exception.ApiError;
import io.swagger.v3.core.converter.AnnotatedType;
import io.swagger.v3.core.converter.ModelConverters;
import io.swagger.v3.core.converter.ResolvedSchema;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class OpenApiConfig implements WebMvcConfigurer {

    private static final String API_ERROR_SCHEMA = "ApiError";
    private static final String JSON = "application/json";

    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        registry.addRedirectViewController("/", "/swagger-ui/index.html");
    }

    @Bean
    public OpenAPI escapaOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Escapa! API")
                        .description("API da plataforma de cursos e qualificação profissional em Turismo e Hospitalidade")
                    .version("v1"))
                .components(new Components()
                    .addSecuritySchemes("bearerAuth", new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("UUID")));
    }

    /** Publica o schema do corpo de erro padrão ({@link ApiError}) para as respostas de erro referenciarem. */
    @Bean
    public OpenApiCustomizer apiErrorSchemaCustomizer() {
        return openApi -> {
            final ResolvedSchema resolved = ModelConverters.getInstance()
                    .resolveAsResolvedSchema(new AnnotatedType(ApiError.class));
            openApi.getComponents().addSchemas(API_ERROR_SCHEMA, resolved.schema);
        };
    }

    /** Documenta em todo endpoint as respostas de erro que o GlobalExceptionHandler pode devolver. */
    @Bean
    public OperationCustomizer standardErrorResponsesCustomizer() {
        return (operation, handlerMethod) -> {
            final String controller = handlerMethod.getBeanType().getSimpleName();
            addError(operation, "400", "Invalid request");
            if (controller.startsWith("Admin")) {
                addError(operation, "403", "Missing, invalid or non-ADMIN X-User-Id header");
            }
            if (controller.startsWith("Student")) {
                addError(operation, "401", "Missing or invalid X-User-Id header");
            }
            if (hasPathParameter(operation)) {
                addError(operation, "404", "Resource not found");
            }
            addError(operation, "500", "Unexpected error");
            return operation;
        };
    }

    private static boolean hasPathParameter(Operation operation) {
        return operation.getParameters() != null
                && operation.getParameters().stream().map(Parameter::getIn).anyMatch("path"::equals);
    }

    private static void addError(Operation operation, String status, String description) {
        if (operation.getResponses().containsKey(status)) {
            return;
        }
        final Schema<?> errorRef = new Schema<>().$ref("#/components/schemas/" + API_ERROR_SCHEMA);
        operation.getResponses().addApiResponse(status, new ApiResponse()
                .description(description)
                .content(new Content().addMediaType(JSON, new MediaType().schema(errorRef))));
    }
}

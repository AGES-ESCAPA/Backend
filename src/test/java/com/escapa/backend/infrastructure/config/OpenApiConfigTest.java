package com.escapa.backend.infrastructure.config;

import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.responses.ApiResponses;
import io.swagger.v3.oas.models.parameters.Parameter;
import org.junit.jupiter.api.Test;
import org.springframework.web.method.HandlerMethod;

import java.lang.reflect.Method;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OpenApiConfigTest {

    private final OpenApiConfig config = new OpenApiConfig();

    @Test
    void shouldAddStandardErrorResponsesForAdminPathOperations() throws Exception {
        final Operation operation = operationWithPathParameter();

        config.standardErrorResponsesCustomizer().customize(operation, handlerMethod(AdminController.class));

        assertTrue(operation.getResponses().containsKey("400"));
        assertTrue(operation.getResponses().containsKey("403"));
        assertTrue(operation.getResponses().containsKey("404"));
        assertTrue(operation.getResponses().containsKey("500"));
        assertTrue(operation.getResponses().containsKey("401"));
        assertTrue(operation.getSecurity().stream().anyMatch(requirement -> requirement.containsKey("bearerAuth")));
    }

    @Test
    void shouldAddUnauthorizedResponseForStudentController() throws Exception {
        final Operation operation = new Operation().responses(new ApiResponses());

        config.standardErrorResponsesCustomizer().customize(operation, handlerMethod(StudentController.class));

        assertTrue(operation.getResponses().containsKey("401"));
        assertFalse(operation.getResponses().containsKey("403"));
        assertFalse(operation.getResponses().containsKey("404"));
    }

    @Test
    void shouldRequireBearerAuthenticationForTheCurrentUserEndpoint() throws Exception {
        final Operation operation = new Operation().responses(new ApiResponses());

        config.standardErrorResponsesCustomizer().customize(operation, handlerMethod(AuthController.class, "me"));

        assertTrue(operation.getResponses().containsKey("401"));
        assertTrue(operation.getSecurity().stream().anyMatch(requirement -> requirement.containsKey("bearerAuth")));
        assertFalse(operation.getResponses().containsKey("403"));
    }

    @Test
    void shouldRequireBearerAuthenticationForAvatarPasswordAndUserEndpoints() throws Exception {
        assertProtectedOperation(AvatarController.class, false);
        assertProtectedOperation(PasswordController.class, false);
        assertProtectedOperation(UserController.class, true);
    }

    @Test
    void shouldRequireBearerAuthenticationAndOwnerAccessForCertificateDownloads() throws Exception {
        final Operation download = new Operation().responses(new ApiResponses());
        config.standardErrorResponsesCustomizer()
                .customize(download, handlerMethod(CertificateController.class, "download"));

        assertTrue(download.getResponses().containsKey("401"));
        assertTrue(download.getResponses().containsKey("403"));
        assertTrue(download.getSecurity().stream().anyMatch(requirement -> requirement.containsKey("bearerAuth")));

        final Operation publicDetails = new Operation().responses(new ApiResponses());
        config.standardErrorResponsesCustomizer()
                .customize(publicDetails, handlerMethod(CertificateController.class, "details"));

        assertFalse(publicDetails.getResponses().containsKey("401"));
        assertFalse(publicDetails.getResponses().containsKey("403"));
        assertFalse(publicDetails.getSecurity() != null && !publicDetails.getSecurity().isEmpty());
    }

    @Test
    void shouldNotAddNotFoundResponseForOperationsWithoutPathParameters() throws Exception {
        final Operation operation = new Operation()
                .responses(new ApiResponses())
                .parameters(List.of(new Parameter().in("query").name("page")));

        config.standardErrorResponsesCustomizer().customize(operation, handlerMethod(PublicController.class));

        assertFalse(operation.getResponses().containsKey("404"));
    }

    @Test
    void shouldLeavePublicAuthenticationOperationsWithoutBearerRequirement() throws Exception {
        final Operation login = new Operation().responses(new ApiResponses());

        config.standardErrorResponsesCustomizer().customize(login, handlerMethod(AuthController.class, "login"));

        assertFalse(login.getResponses().containsKey("401"));
        assertFalse(login.getSecurity() != null && !login.getSecurity().isEmpty());
    }

    @Test
    void shouldPreserveAnExplicitResponseAndDocumentApiErrorSchema() throws Exception {
        final ApiResponses responses = new ApiResponses().addApiResponse("400",
                new io.swagger.v3.oas.models.responses.ApiResponse().description("Custom validation"));
        final Operation operation = new Operation().responses(responses);
        final var openApi = config.escapaOpenApi();

        config.standardErrorResponsesCustomizer().customize(operation, handlerMethod(PublicController.class));
        config.apiErrorSchemaCustomizer().customise(openApi);

        assertEquals("Custom validation", operation.getResponses().get("400").getDescription());
        assertFalse(operation.getResponses().containsKey("401"));
        assertFalse(operation.getSecurity() != null && !operation.getSecurity().isEmpty());
        assertTrue(openApi.getComponents().getSchemas().containsKey("ApiError"));
    }

    private void assertProtectedOperation(Class<?> controllerType, boolean requiresForbidden)
            throws NoSuchMethodException {
        final Operation operation = new Operation().responses(new ApiResponses());
        config.standardErrorResponsesCustomizer().customize(operation, handlerMethod(controllerType));

        assertTrue(operation.getResponses().containsKey("401"));
        assertEquals(requiresForbidden, operation.getResponses().containsKey("403"));
        assertTrue(operation.getSecurity().stream().anyMatch(requirement -> requirement.containsKey("bearerAuth")));
    }

    private Operation operationWithPathParameter() {
        return new Operation()
                .responses(new ApiResponses())
                .parameters(List.of(new Parameter().in("path").name("id")));
    }

    private HandlerMethod handlerMethod(Class<?> controllerType) throws NoSuchMethodException {
        return handlerMethod(controllerType, "handle");
    }

    private HandlerMethod handlerMethod(Class<?> controllerType, String methodName) throws NoSuchMethodException {
        final Object controller = instantiate(controllerType);
        final Method method = controllerType.getMethod(methodName);
        return new HandlerMethod(controller, method);
    }

    private Object instantiate(Class<?> controllerType) {
        try {
            return controllerType.getDeclaredConstructor().newInstance();
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException(exception);
        }
    }

    static class AdminController {
        public void handle() {
        }
    }

    static class StudentController {
        public void handle() {
        }
    }

    static class PublicController {
        public void handle() {
        }
    }

    static class AuthController {
        public void me() {
        }

        public void login() {
        }
    }

    static class AvatarController {
        public void handle() {
        }
    }

    static class PasswordController {
        public void handle() {
        }
    }

    static class UserController {
        public void handle() {
        }
    }

    static class CertificateController {
        public void download() {
        }

        public void details() {
        }
    }
}

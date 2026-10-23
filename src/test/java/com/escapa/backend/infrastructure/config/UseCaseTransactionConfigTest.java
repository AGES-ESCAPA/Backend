package com.escapa.backend.infrastructure.config;

import com.escapa.backend.application.usecase.CreateUserUseCase;
import com.escapa.backend.application.usecase.DownloadCertificateUseCase;
import com.escapa.backend.application.usecase.ListUsersUseCase;
import com.escapa.backend.application.usecase.PublishCourseUseCase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.aop.Advisor;
import org.springframework.aop.PointcutAdvisor;
import org.springframework.transaction.TransactionManager;
import org.springframework.transaction.interceptor.TransactionAttribute;
import org.springframework.transaction.interceptor.TransactionInterceptor;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import java.lang.reflect.Method;
import java.util.Arrays;

class UseCaseTransactionConfigTest {

    private Advisor useCaseTransactionAdvisor;

    @BeforeEach
    void setUp() {
        this.useCaseTransactionAdvisor =
                new UseCaseTransactionConfig().useCaseTransactionAdvisor(mock(TransactionManager.class));
    }

    @Test
    void shouldAdviseUseCaseExecuteMethods() {
        final Method execute = executeMethod(ListUsersUseCase.class);
        assertTrue(((PointcutAdvisor) useCaseTransactionAdvisor).getPointcut().getMethodMatcher()
                .matches(execute, ListUsersUseCase.class));
    }

    @Test
    void shouldRunQueryUseCasesInReadOnlyTransactions() throws NoSuchMethodException {
        assertTrue(attributeOf(ListUsersUseCase.class).isReadOnly());
        assertTrue(attributeOf(com.escapa.backend.application.usecase.AuthorizeAdminUseCase.class).isReadOnly());
    }

    @Test
    void shouldRunCommandUseCasesInWritableTransactions() throws NoSuchMethodException {
        assertFalse(attributeOf(CreateUserUseCase.class).isReadOnly());
        assertFalse(attributeOf(PublishCourseUseCase.class).isReadOnly());
        // Download looks like a query but stores the generated PDF in the cache column.
        assertFalse(attributeOf(DownloadCertificateUseCase.class).isReadOnly());
    }

    private TransactionAttribute attributeOf(Class<?> useCaseType) throws NoSuchMethodException {
        final TransactionInterceptor interceptor = (TransactionInterceptor) useCaseTransactionAdvisor.getAdvice();
        final Method execute = executeMethod(useCaseType);
        final TransactionAttribute attribute = interceptor.getTransactionAttributeSource()
                .getTransactionAttribute(execute, useCaseType);
        assertNotNull(attribute);
        return attribute;
    }

    private Method executeMethod(Class<?> useCaseType) {
        return Arrays.stream(useCaseType.getMethods())
                .filter(method -> method.getName().equals("execute"))
                .findFirst()
                .orElseThrow();
    }
}

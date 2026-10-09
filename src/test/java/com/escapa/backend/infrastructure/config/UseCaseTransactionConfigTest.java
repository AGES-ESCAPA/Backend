package com.escapa.backend.infrastructure.config;

import com.escapa.backend.application.usecase.CreateUserUseCase;
import com.escapa.backend.application.usecase.DownloadCertificateUseCase;
import com.escapa.backend.application.usecase.ListUsersUseCase;
import com.escapa.backend.application.usecase.PublishCourseUseCase;
import com.escapa.backend.infrastructure.persistence.PostgresIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.aop.Advisor;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.interceptor.TransactionAttribute;
import org.springframework.transaction.interceptor.TransactionInterceptor;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UseCaseTransactionConfigTest extends PostgresIntegrationTest {

    @Autowired
    private Advisor useCaseTransactionAdvisor;

    @Autowired
    private ListUsersUseCase listUsersUseCase;

    @Autowired
    private CreateUserUseCase createUserUseCase;

    @Test
    void shouldWrapUseCasesInTransactionalProxies() {
        assertTrue(AopUtils.isAopProxy(listUsersUseCase));
        assertTrue(AopUtils.isAopProxy(createUserUseCase));
    }

    @Test
    void shouldRunQueryUseCasesInReadOnlyTransactions() throws NoSuchMethodException {
        assertTrue(attributeOf(ListUsersUseCase.class).isReadOnly());
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
        final java.lang.reflect.Method execute = useCaseType.getMethods()[0];
        final TransactionAttribute attribute = interceptor.getTransactionAttributeSource()
                .getTransactionAttribute(execute, useCaseType);
        assertNotNull(attribute);
        return attribute;
    }
}

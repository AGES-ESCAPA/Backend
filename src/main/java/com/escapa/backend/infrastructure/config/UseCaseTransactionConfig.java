package com.escapa.backend.infrastructure.config;

import org.springframework.aop.Advisor;
import org.springframework.aop.aspectj.AspectJExpressionPointcut;
import org.springframework.aop.support.DefaultPointcutAdvisor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.TransactionManager;
import org.springframework.transaction.interceptor.RuleBasedTransactionAttribute;
import org.springframework.transaction.interceptor.TransactionAttributeSource;
import org.springframework.transaction.interceptor.TransactionInterceptor;

import java.util.List;

/**
 * Define a fronteira transacional de cada caso de uso sem acoplar a camada {@code application}
 * ao Spring: um único advice envolve o {@code execute} de toda classe {@code *UseCase}.
 *
 * <p>Casos de uso de consulta (prefixos {@code Get}, {@code List}, {@code Search} e
 * {@code Authorize}) rodam em transação somente leitura; os demais, em transação de escrita,
 * de modo que seus passos (ex.: salvar o curso e o histórico de alterações) confirmam ou
 * revertem juntos. Um caso de uso que grave dados não deve usar esses prefixos.
 */
@Configuration
public class UseCaseTransactionConfig {

    private static final String USE_CASE_EXECUTION =
            "execution(public * com.escapa.backend.application.usecase.*UseCase.execute(..))";

    private static final List<String> READ_ONLY_PREFIXES = List.of("Get", "List", "Search", "Authorize");

    @Bean
    public Advisor useCaseTransactionAdvisor(TransactionManager transactionManager) {
        final TransactionAttributeSource attributeSource = (method, targetClass) -> {
            final RuleBasedTransactionAttribute attribute = new RuleBasedTransactionAttribute();
            attribute.setReadOnly(targetClass != null && isReadOnly(targetClass.getSimpleName()));
            return attribute;
        };

        final AspectJExpressionPointcut pointcut = new AspectJExpressionPointcut();
        pointcut.setExpression(USE_CASE_EXECUTION);

        return new DefaultPointcutAdvisor(pointcut, new TransactionInterceptor(transactionManager, attributeSource));
    }

    private static boolean isReadOnly(String useCaseName) {
        return READ_ONLY_PREFIXES.stream().anyMatch(useCaseName::startsWith);
    }
}

package com.escapa.backend.infrastructure.persistence;

import java.util.UUID;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import com.escapa.backend.application.port.FileStoragePort;
import com.escapa.backend.application.port.RegularUserRepositoryPort;
import com.escapa.backend.application.port.TokenProviderPort;
import com.escapa.backend.application.model.TokenClaims;
import com.escapa.backend.application.usecase.InMemoryFileStoragePort;
import com.escapa.backend.domain.entity.User;
import com.escapa.backend.infrastructure.security.JwtTokenProviderAdapter;

@TestConfiguration(proxyBeanMethods = false)
class StudentRegistrationTestConfig {

    @Bean
    @Primary
    RegistrationRepositoryControl registrationRepositoryControl(
            @Qualifier("regularUserRepositoryPort") RegularUserRepositoryPort delegate) {
        return new RegistrationRepositoryControl(delegate);
    }

    @Bean
    @Primary
    RegistrationTokenControl registrationTokenControl(JwtTokenProviderAdapter delegate) {
        return new RegistrationTokenControl(delegate);
    }

    @Bean
    @Primary
    FileStoragePort registrationTestStorage() {
        return new InMemoryFileStoragePort();
    }

    /** Sincroniza duas requisicoes depois das consultas de unicidade, mantendo a gravacao real. */
    static class RegistrationRepositoryControl implements RegularUserRepositoryPort {

        private final RegularUserRepositoryPort delegate;
        private volatile CyclicBarrier saveBarrier;

        RegistrationRepositoryControl(RegularUserRepositoryPort delegate) {
            this.delegate = delegate;
        }

        void synchronizeSaves() {
            saveBarrier = new CyclicBarrier(2);
        }

        void reset() {
            saveBarrier = null;
        }

        @Override
        public User save(User user, String cpf, String phone) {
            final CyclicBarrier barrier = saveBarrier;
            if (barrier != null) {
                await(barrier);
            }
            return delegate.save(user, cpf, phone);
        }

        @Override
        public boolean existsByCpf(String cpf) {
            return delegate.existsByCpf(cpf);
        }

        private static void await(CyclicBarrier barrier) {
            try {
                barrier.await(20, TimeUnit.SECONDS);
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("Interrupted registration race", exception);
            } catch (BrokenBarrierException | TimeoutException exception) {
                throw new IllegalStateException("Registration race did not reach both saves", exception);
            }
        }
    }

    /** Delega o JWT real; a falha controlada ocorre depois de salvar o usuario e seu perfil. */
    static class RegistrationTokenControl implements TokenProviderPort {

        private final JwtTokenProviderAdapter delegate;
        private volatile boolean failGeneration;
        private volatile UUID attemptedUserId;

        RegistrationTokenControl(JwtTokenProviderAdapter delegate) {
            this.delegate = delegate;
        }

        void failGeneration() {
            failGeneration = true;
        }

        UUID attemptedUserId() {
            return attemptedUserId;
        }

        void reset() {
            failGeneration = false;
            attemptedUserId = null;
        }

        @Override
        public String generateToken(UUID userId, String email, String role, String profile) {
            attemptedUserId = userId;
            if (failGeneration) {
                throw new IllegalStateException("Token generation failed for the test");
            }
            return delegate.generateToken(userId, email, role, profile);
        }

        @Override
        public TokenClaims validateToken(String token) {
            return delegate.validateToken(token);
        }

        @Override
        public long getExpirationSeconds() {
            return delegate.getExpirationSeconds();
        }
    }
}

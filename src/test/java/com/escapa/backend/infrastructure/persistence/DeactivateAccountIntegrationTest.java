package com.escapa.backend.infrastructure.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.escapa.backend.application.port.PasswordHasherPort;
import com.escapa.backend.application.port.UserRepositoryPort;
import com.escapa.backend.application.usecase.DeactivateAccountUseCase;
import com.escapa.backend.domain.entity.User;
import com.escapa.backend.domain.user.InvalidCurrentPasswordException;
import com.escapa.backend.domain.user.UserAlreadyInactiveException;
import com.escapa.backend.domain.user.UserStatus;

class DeactivateAccountIntegrationTest extends PostgresIntegrationTest {

    private static final String PASSWORD = "senha1234";

    @Autowired
    private DeactivateAccountUseCase deactivateAccountUseCase;

    @Autowired
    private UserRepositoryPort userRepositoryPort;

    @Autowired
    private PasswordHasherPort passwordHasher;

    private User saveStudent(String email) {
        return userRepositoryPort.save(
                new User("Aluno Real", email, passwordHasher.hash(PASSWORD), "STUDENT"));
    }

    @Test
    void shouldPersistInactiveStatusInDatabaseWhenAccountIsDeactivated() {
        final User student = saveStudent("desativa.ok@email.com");
        assertEquals(UserStatus.ACTIVE, userRepositoryPort.findById(student.getId()).orElseThrow().getStatus());

        deactivateAccountUseCase.execute(student.getId(), PASSWORD);

        final User reloaded = userRepositoryPort.findById(student.getId()).orElseThrow();
        assertEquals(UserStatus.INACTIVE, reloaded.getStatus());
    }

    @Test
    void shouldRejectSecondDeactivationOfTheSameAccount() {
        final User student = saveStudent("desativa.duas@email.com");
        deactivateAccountUseCase.execute(student.getId(), PASSWORD);

        assertThrows(UserAlreadyInactiveException.class,
                () -> deactivateAccountUseCase.execute(student.getId(), PASSWORD));
    }

    @Test
    void shouldKeepAccountActiveWhenCurrentPasswordIsWrong() {
        final User student = saveStudent("desativa.senha@email.com");

        assertThrows(InvalidCurrentPasswordException.class,
                () -> deactivateAccountUseCase.execute(student.getId(), "senha-errada"));

        assertEquals(UserStatus.ACTIVE, userRepositoryPort.findById(student.getId()).orElseThrow().getStatus());
    }
}
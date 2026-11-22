package com.escapa.backend.application.usecase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.escapa.backend.application.model.UserProfile;
import com.escapa.backend.domain.entity.Company;
import com.escapa.backend.domain.entity.User;
import com.escapa.backend.domain.user.UserNotFoundException;

class AdminUpdateUserUseCaseTest {

    private InMemoryUserRepositoryPort users;
    private InMemoryCompanyRepositoryPort companies;
    private InMemoryUsersCompanyRepositoryPort links;
    private InMemoryUserProfileChangeLogRepositoryPort changeLog;
    private AdminUpdateUserUseCase useCase;
    private UUID actorId;

    @BeforeEach
    void setUp() {
        users = new InMemoryUserRepositoryPort();
        companies = new InMemoryCompanyRepositoryPort(users);
        links = new InMemoryUsersCompanyRepositoryPort();
        changeLog = new InMemoryUserProfileChangeLogRepositoryPort();
        useCase = new AdminUpdateUserUseCase(users, companies, links, changeLog);
        actorId = users.save(new User("Admin", "admin@escapa.com", "hash", "ADMIN")).getId();
    }

    @Test
    void shouldUpdateStudentNameAndPhone() {
        final User student = users.save(new User("Ana", "ana@escapa.com", "hash", "STUDENT"));
        users.saveStudentContact(student.getId(), "39011122233", "51991203344");

        final UserProfile updated = useCase.execute(
                actorId, student.getId(), "Ana Nova", "(51) 99999-8888", null);

        assertEquals("Ana Nova", updated.name());
        assertEquals("51999998888", updated.phone());
        assertTrue(changeLog.entries().stream().anyMatch(entry -> "name".equals(entry.fieldName())));
        assertTrue(changeLog.entries().stream().anyMatch(entry -> "phone".equals(entry.fieldName())));
    }

    @Test
    void shouldUpdateAdminNameWithoutPhone() {
        final User admin = users.save(new User("Barbara", "barbara@escapa.com", "hash", "ADMIN"));

        final UserProfile updated = useCase.execute(actorId, admin.getId(), "Barbara Nova", "51999998888", null);

        assertEquals("Barbara Nova", updated.name());
        assertEquals(1, changeLog.entries().size());
        assertEquals("name", changeLog.entries().get(0).fieldName());
    }

    @Test
    void shouldUpdateCompanyNameAndPhone() {
        final UUID companyId = UUID.randomUUID();
        companies.save(
                new User(companyId, "Hotel Contato", "hotel@escapa.com", "hash", "COMPANY", null, null),
                new Company(companyId, "Hotel Antigo", "11222333000181", "hotel@escapa.com", "5133334444"));

        final UserProfile updated = useCase.execute(
                actorId, companyId, "Hotel Contato", "5133335555", "Hotel Novo");

        assertEquals("Hotel Contato", updated.name());
        assertEquals("Hotel Novo", updated.companyName());
        assertEquals("5133335555", updated.phone());
    }

    @Test
    void shouldRejectBlankNameAndInvalidPhone() {
        final User student = users.save(new User("Ana", "ana@escapa.com", "hash", "STUDENT"));
        users.saveStudentContact(student.getId(), "39011122233", "51991203344");

        assertThrows(IllegalArgumentException.class,
                () -> useCase.execute(actorId, student.getId(), "  ", "51991203344", null));
        assertThrows(IllegalArgumentException.class,
                () -> useCase.execute(actorId, student.getId(), "Ana", "123", null));
    }

    @Test
    void shouldRejectUnknownUser() {
        assertThrows(UserNotFoundException.class,
                () -> useCase.execute(actorId, UUID.randomUUID(), "Nome", null, null));
    }
}

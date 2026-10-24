package com.escapa.backend.application.usecase;

import com.escapa.backend.application.model.UserProfile;
import com.escapa.backend.domain.entity.Company;
import com.escapa.backend.domain.entity.User;
import com.escapa.backend.domain.user.UserNotFoundException;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GetProfileUseCaseTest {

    private final InMemoryUserRepositoryPort userRepository = new InMemoryUserRepositoryPort();
    private final InMemoryCompanyRepositoryPort companyRepository = new InMemoryCompanyRepositoryPort(userRepository);
    private final InMemoryUsersCompanyRepositoryPort usersCompanyRepository = new InMemoryUsersCompanyRepositoryPort();
    private final GetProfileUseCase useCase =
            new GetProfileUseCase(userRepository, companyRepository, usersCompanyRepository);

    private User saveStudent() {
        final User student = userRepository.save(new User("Mariana Costa", "mariana@email.com", "hash", "STUDENT"));
        userRepository.saveStudentContact(student.getId(), "39011122233", "51991203344");
        return student;
    }

    private UUID saveCompany() {
        final UUID id = UUID.randomUUID();
        companyRepository.save(
                new User(id, "Ana Responsavel", "contato@vistamar.com.br", "hash", "COMPANY", null, null),
                new Company(id, "Pousada Vista Mar LTDA", "12345678000191", "contato@vistamar.com.br", "51999990001"));
        return id;
    }

    @Test
    void shouldReturnStudentProfileWithCpfAndPhone() {
        final User student = saveStudent();

        final UserProfile profile = useCase.execute(student.getId());

        assertEquals("STUDENT", profile.profile());
        assertTrue(profile.editable());
        assertEquals("Mariana Costa", profile.name());
        assertEquals("mariana@email.com", profile.email());
        assertEquals("39011122233", profile.cpf());
        assertEquals("51991203344", profile.phone());
        assertNull(profile.companyName());
        assertNull(profile.cnpj());
        assertNull(profile.avatarUrl());
    }

    @Test
    void shouldReturnSavedAvatarUrlForStudentCompanyAndAdmin() {
        final User student = saveStudent();
        student.setAvatarUrl("http://minio/avatars/mariana.png");
        final UUID companyId = UUID.randomUUID();
        companyRepository.save(
                new User(companyId, "Ana Responsavel", "contato@vistamar.com.br", "hash", "COMPANY",
                        "http://minio/avatars/vistamar.png", null),
                new Company(companyId, "Pousada Vista Mar LTDA", "12345678000191", null, null));
        final User admin = userRepository.save(new User("Barbara Diogo", "barbara@escapa.com.br", "hash", "ADMIN"));
        admin.setAvatarUrl("http://minio/avatars/barbara.png");

        assertEquals("http://minio/avatars/mariana.png", useCase.execute(student.getId()).avatarUrl());
        assertEquals("http://minio/avatars/vistamar.png", useCase.execute(companyId).avatarUrl());
        assertEquals("http://minio/avatars/barbara.png", useCase.execute(admin.getId()).avatarUrl());
    }

    @Test
    void shouldReturnEmployeeProfileAsReadOnlyWithEmployerName() {
        final User employee = saveStudent();
        usersCompanyRepository.linkToCompany(employee.getId(), saveCompany());

        final UserProfile profile = useCase.execute(employee.getId());

        assertEquals("EMPLOYEE", profile.profile());
        assertFalse(profile.editable());
        assertEquals("39011122233", profile.cpf());
        assertEquals("51991203344", profile.phone());
        assertEquals("Pousada Vista Mar LTDA", profile.companyName());
        assertNull(profile.cnpj());
    }

    @Test
    void shouldReturnCompanyProfileWithResponsibleNameAndCnpj() {
        final UUID companyId = saveCompany();

        final UserProfile profile = useCase.execute(companyId);

        assertEquals("COMPANY", profile.profile());
        assertTrue(profile.editable());
        assertEquals("Ana Responsavel", profile.name());
        assertEquals("contato@vistamar.com.br", profile.email());
        assertEquals("Pousada Vista Mar LTDA", profile.companyName());
        assertEquals("12345678000191", profile.cnpj());
        assertEquals("51999990001", profile.phone());
        assertNull(profile.cpf());
    }

    @Test
    void shouldReturnStudentWithoutContactRowWithNullCpfAndPhone() {
        final User student = userRepository.save(new User("Sem Contato", "sem@email.com", "hash", "STUDENT"));

        final UserProfile profile = useCase.execute(student.getId());

        assertEquals("STUDENT", profile.profile());
        assertNull(profile.cpf());
        assertNull(profile.phone());
    }

    @Test
    void shouldReturnAdminProfileAsReadOnly() {
        final User admin = userRepository.save(new User("Barbara Diogo", "barbara@escapa.com.br", "hash", "ADMIN"));

        final UserProfile profile = useCase.execute(admin.getId());

        assertEquals("ADMIN", profile.profile());
        assertFalse(profile.editable());
        assertEquals("Barbara Diogo", profile.name());
    }

    @Test
    void shouldThrowWhenUserDoesNotExist() {
        assertThrows(UserNotFoundException.class, () -> useCase.execute(UUID.randomUUID()));
    }
}

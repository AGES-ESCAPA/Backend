package com.escapa.backend.application.usecase;

import com.escapa.backend.application.model.UserProfile;
import com.escapa.backend.domain.entity.Company;
import com.escapa.backend.domain.entity.User;
import com.escapa.backend.domain.user.ProfileNotEditableException;
import com.escapa.backend.domain.user.UserNotFoundException;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class UpdateProfileUseCaseTest {

    private final InMemoryUserRepositoryPort userRepository = new InMemoryUserRepositoryPort();
    private final InMemoryCompanyRepositoryPort companyRepository = new InMemoryCompanyRepositoryPort(userRepository);
    private final InMemoryUsersCompanyRepositoryPort usersCompanyRepository = new InMemoryUsersCompanyRepositoryPort();
    private final UpdateProfileUseCase useCase =
            new UpdateProfileUseCase(userRepository, companyRepository, usersCompanyRepository);

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
    void shouldUpdateStudentNameAndPhoneKeepingEmailAndCpf() {
        final User student = saveStudent();

        final UserProfile profile = useCase.execute(student.getId(), "  Mariana Souza  ", "(54) 99999-8888", null);

        assertEquals("Mariana Souza", profile.name());
        assertEquals("54999998888", profile.phone());
        assertEquals("mariana@email.com", profile.email());
        assertEquals("39011122233", profile.cpf());
        assertEquals("Mariana Souza", userRepository.findById(student.getId()).orElseThrow().getName());
    }

    @Test
    void shouldIgnoreCompanyNameForStudent() {
        final User student = saveStudent();

        final UserProfile profile = useCase.execute(student.getId(), "Mariana Costa", null, "Empresa Qualquer");

        assertEquals("STUDENT", profile.profile());
        assertNull(profile.companyName());
    }

    @Test
    void shouldClearStudentPhoneWhenOmitted() {
        final User student = saveStudent();

        final UserProfile profile = useCase.execute(student.getId(), "Mariana Costa", "  ", null);

        assertNull(profile.phone());
        assertEquals("39011122233", profile.cpf());
    }

    @Test
    void shouldUpdateCompanyResponsibleLegalNameAndPhoneKeepingCnpj() {
        final UUID companyId = saveCompany();

        final UserProfile profile = useCase.execute(companyId, "Joao Responsavel", "51 3333-4444", "Vista Mar S.A.");

        assertEquals("Joao Responsavel", profile.name());
        assertEquals("Vista Mar S.A.", profile.companyName());
        assertEquals("5133334444", profile.phone());
        assertEquals("12345678000191", profile.cnpj());
        assertEquals("contato@vistamar.com.br", profile.email());
        assertEquals("Vista Mar S.A.", companyRepository.find(companyId).getCompanyName());
    }

    @Test
    void shouldRequireCompanyNameForCompany() {
        final UUID companyId = saveCompany();

        final IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> useCase.execute(companyId, "Joao Responsavel", null, " "));

        assertEquals("companyName is required", error.getMessage());
        assertEquals("Pousada Vista Mar LTDA", companyRepository.find(companyId).getCompanyName());
    }

    @Test
    void shouldForbidEmployeeFromEditing() {
        final User employee = saveStudent();
        usersCompanyRepository.linkToCompany(employee.getId(), saveCompany());

        assertThrows(ProfileNotEditableException.class,
                () -> useCase.execute(employee.getId(), "Outro Nome", null, null));

        assertEquals("Mariana Costa", userRepository.findById(employee.getId()).orElseThrow().getName());
        assertEquals("51991203344", userRepository.findStudentContact(employee.getId()).orElseThrow().phone());
    }

    @Test
    void shouldForbidAdminFromEditing() {
        final User admin = userRepository.save(new User("Barbara Diogo", "barbara@escapa.com.br", "hash", "ADMIN"));

        assertThrows(ProfileNotEditableException.class,
                () -> useCase.execute(admin.getId(), "Outro Nome", null, null));
    }

    @Test
    void shouldRejectBlankName() {
        final User student = saveStudent();

        final IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> useCase.execute(student.getId(), "   ", null, null));

        assertEquals("name is required", error.getMessage());
        assertEquals("Mariana Costa", userRepository.findById(student.getId()).orElseThrow().getName());
    }

    @Test
    void shouldRejectPhoneWithWrongNumberOfDigits() {
        final User student = saveStudent();

        final IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> useCase.execute(student.getId(), "Mariana Costa", "54 9999", null));

        assertEquals("phone is invalid", error.getMessage());
        assertEquals("51991203344", userRepository.findStudentContact(student.getId()).orElseThrow().phone());
    }

    @Test
    void shouldThrowWhenUserDoesNotExist() {
        assertThrows(UserNotFoundException.class,
                () -> useCase.execute(UUID.randomUUID(), "Nome", null, null));
    }
}

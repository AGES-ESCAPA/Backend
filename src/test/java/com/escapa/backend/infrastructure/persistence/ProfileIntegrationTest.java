package com.escapa.backend.infrastructure.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.time.LocalDateTime;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.escapa.backend.application.model.UserProfile;
import com.escapa.backend.application.port.CompanyRepositoryPort;
import com.escapa.backend.application.usecase.GetProfileUseCase;
import com.escapa.backend.application.usecase.UpdateProfileUseCase;
import com.escapa.backend.domain.entity.Company;
import com.escapa.backend.domain.entity.User;
import com.escapa.backend.infrastructure.persistence.entity.RegularUserEntity;
import com.escapa.backend.infrastructure.persistence.entity.UsersCompanyEntity;
import com.escapa.backend.infrastructure.persistence.entity.UsersCompanyId;

class ProfileIntegrationTest extends PostgresIntegrationTest {

    @Autowired
    private GetProfileUseCase getProfileUseCase;

    @Autowired
    private UpdateProfileUseCase updateProfileUseCase;

    @Autowired
    private UserJpaRepository userJpaRepository;

    @Autowired
    private CompanyJpaRepository companyJpaRepository;

    @Autowired
    private UsersCompanyJpaRepository usersCompanyJpaRepository;

    @Autowired
    private CompanyRepositoryPort companyRepositoryPort;

    private final UUID studentId = UUID.randomUUID();
    private final UUID companyId = UUID.randomUUID();

    @AfterEach
    void tearDown() {
        usersCompanyJpaRepository.deleteAll(usersCompanyJpaRepository.findAll().stream()
                .filter(link -> studentId.equals(link.getId().getUserId()))
                .toList());
        userJpaRepository.findById(studentId).ifPresent(userJpaRepository::delete);
        userJpaRepository.findById(companyId).ifPresent(userJpaRepository::delete);
    }

    private void saveRegularStudent(String phone) {
        userJpaRepository.save(new RegularUserEntity(studentId, "Aluno Perfil", studentId + "@email.com",
                "hash", "STUDENT", LocalDateTime.now(), "390" + studentId.toString().substring(0, 8), phone));
    }

    private void saveCompany() {
        companyRepositoryPort.save(
                new User(companyId, "Responsavel Perfil", companyId + "@empresa.com.br", "hash", "COMPANY", null,
                        LocalDateTime.now()),
                new Company(companyId, "Empresa Perfil LTDA", "1" + System.nanoTime(), null, "51999990001"));
    }

    @Test
    void shouldCreateRegularUserRowWhenStudentWithoutOneSavesPhone() {
        userJpaRepository.save(new UserEntity(studentId, "Aluno Antigo", studentId + "@email.com", "hash",
                "STUDENT", LocalDateTime.now()));

        final UserProfile updated = updateProfileUseCase.execute(studentId, "Aluno Atualizado", "54999998888", null);

        assertEquals("54999998888", updated.phone());
        assertEquals("Aluno Atualizado", updated.name());
        final UserProfile reloaded = getProfileUseCase.execute(studentId);
        assertEquals("54999998888", reloaded.phone());
        assertNull(reloaded.cpf());
        assertEquals("Aluno Atualizado", reloaded.name());
    }

    @Test
    void shouldUpdatePhoneOfExistingRegularUserKeepingCpf() {
        saveRegularStudent("+55 51 99120-3344");
        final String cpf = getProfileUseCase.execute(studentId).cpf();

        updateProfileUseCase.execute(studentId, "Aluno Perfil", "(51) 98765-4321", null);

        final UserProfile reloaded = getProfileUseCase.execute(studentId);
        assertEquals("51987654321", reloaded.phone());
        assertEquals(cpf, reloaded.cpf());
    }

    @Test
    void shouldUpdateCompanyDataAndKeepCnpj() {
        saveCompany();
        final String cnpj = getProfileUseCase.execute(companyId).cnpj();

        updateProfileUseCase.execute(companyId, "Novo Responsavel", "5133334444", "Empresa Renomeada S.A.");

        final UserProfile reloaded = getProfileUseCase.execute(companyId);
        assertEquals("Novo Responsavel", reloaded.name());
        assertEquals("Empresa Renomeada S.A.", reloaded.companyName());
        assertEquals("5133334444", reloaded.phone());
        assertEquals(cnpj, reloaded.cnpj());
    }

    @Test
    void shouldResolveEmployeeAndEmployerNameFromUsersCompany() {
        saveRegularStudent("54999998888");
        saveCompany();
        usersCompanyJpaRepository.save(new UsersCompanyEntity(new UsersCompanyId(studentId, companyId),
                userJpaRepository.findById(studentId).orElseThrow(),
                companyJpaRepository.findById(companyId).orElseThrow(),
                "RECEPCAO"));

        final UserProfile profile = getProfileUseCase.execute(studentId);

        assertEquals("EMPLOYEE", profile.profile());
        assertEquals("Empresa Perfil LTDA", profile.companyName());
    }
}

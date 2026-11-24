package com.escapa.backend.application.usecase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.escapa.backend.application.model.UserProfile;
import com.escapa.backend.domain.entity.Company;
import com.escapa.backend.domain.entity.User;
import com.escapa.backend.domain.user.UserNotFoundException;
import com.escapa.backend.domain.user.UserTypes;

class UserProfileReaderTest {

    private final InMemoryUserRepositoryPort users = new InMemoryUserRepositoryPort();
    private final InMemoryCompanyRepositoryPort companies = new InMemoryCompanyRepositoryPort(users);
    private final InMemoryUsersCompanyRepositoryPort usersCompany = new InMemoryUsersCompanyRepositoryPort();
    private final UserProfileReader reader = new UserProfileReader(users, companies, usersCompany);

    @Test
    void shouldRequireExistingUser() {
        final UUID missingId = UUID.randomUUID();

        assertThrows(UserNotFoundException.class, () -> reader.requireUser(missingId));
    }

    @Test
    void shouldResolveEmployeeProfileWhenStudentIsLinkedToCompany() {
        final User student = users.save(new User("Luciana", "luciana@empresa.com", "hash", UserTypes.STUDENT));
        final UUID companyId = UUID.randomUUID();
        companies.save(
                new User(companyId, "Ana", "rh@empresa.com", "hash", UserTypes.COMPANY, null, null),
                new Company(companyId, "Hotel Gramado", "11222333000144", "rh@empresa.com", null));
        usersCompany.linkToCompany(student.getId(), companyId);

        assertEquals(UserProfileReader.EMPLOYEE, reader.resolveProfile(student));
        assertFalse(UserProfileReader.isEditable(UserProfileReader.EMPLOYEE));
    }

    @Test
    void shouldMarkStudentCompanyAndAdminAsEditable() {
        assertTrue(UserProfileReader.isEditable(UserTypes.STUDENT));
        assertTrue(UserProfileReader.isEditable(UserTypes.COMPANY));
        assertTrue(UserProfileReader.isEditable(UserTypes.ADMIN));
        assertFalse(UserProfileReader.isEditable("UNKNOWN"));
    }

    @Test
    void shouldReadAdminInstructorFieldsWhenPresent() {
        final User admin = users.save(new User("Barbara", "barbara@escapa.com", "hash", UserTypes.ADMIN));
        users.saveAdminInstructorProfile(admin.getId(), "Headline", "Bio longa");

        final UserProfile profile = reader.read(admin);

        assertEquals(UserTypes.ADMIN, profile.profile());
        assertTrue(profile.editable());
        assertEquals("Headline", profile.headline());
        assertEquals("Bio longa", profile.bio());
    }

    @Test
    void shouldReturnGenericProfileForUnknownUserType() {
        final User unknown = users.save(new User("Outro", "outro@escapa.com", "hash", "SUPPORT"));
        unknown.setAvatarUrl("http://minio/avatars/outro.png");

        final UserProfile profile = reader.read(unknown);

        assertEquals("SUPPORT", profile.profile());
        assertFalse(profile.editable());
        assertEquals("Outro", profile.name());
        assertEquals("outro@escapa.com", profile.email());
        assertEquals("http://minio/avatars/outro.png", profile.avatarUrl());
        assertNull(profile.cpf());
        assertNull(profile.companyName());
    }

    @Test
    void shouldReturnNullEmployerNameWhenCompanyLinkIsBroken() {
        final User employee = users.save(new User("Luciana", "luciana@empresa.com", "hash", UserTypes.STUDENT));
        users.saveStudentContact(employee.getId(), "39011122233", "51991203344");
        usersCompany.linkToCompany(employee.getId(), UUID.randomUUID());

        final UserProfile profile = reader.read(employee);

        assertEquals(UserProfileReader.EMPLOYEE, profile.profile());
        assertNull(profile.companyName());
        assertEquals("39011122233", profile.cpf());
    }
}

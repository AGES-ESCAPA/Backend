package com.escapa.backend.user.repository;

import com.escapa.backend.common.JpaIntegrationTest;
import com.escapa.backend.user.entity.AdminEntity;
import com.escapa.backend.user.entity.RegularUserEntity;
import com.escapa.backend.user.entity.UserEntity;
import com.escapa.backend.user.entity.UserRole;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UserRepositoryTest extends JpaIntegrationTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TestEntityManager em;

    @Test
    void shouldPersistAndReadBackAllColumns() {
        final UserEntity saved = userRepository.saveAndFlush(newStudent("maria.persist@email.com"));

        final Optional<UserEntity> found = userRepository.findById(saved.getId());

        assertTrue(found.isPresent());
        assertEquals("Maria Persist", found.get().getName());
        assertEquals("maria.persist@email.com", found.get().getEmail());
        assertEquals(UserRole.STUDENT, found.get().getRole());
        assertEquals("hash", found.get().getPasswordHash());
        assertEquals(saved.getCreatedAt(), found.get().getCreatedAt());
    }

    @Test
    void shouldStoreRoleAsTextMatchingTheSeedVocabulary() {
        final UserEntity saved = userRepository.saveAndFlush(newStudent("role.text@email.com"));

        final Object raw = em.getEntityManager()
                .createNativeQuery("SELECT role FROM users WHERE id = :id")
                .setParameter("id", saved.getId())
                .getSingleResult();

        assertEquals("STUDENT", raw);
    }

    @Test
    void savingAnAdminEntityShouldWriteTheAdminsRowRequiredByTheInstructorForeignKey() {
        final AdminEntity admin = new AdminEntity(UUID.randomUUID(), "Bia", "bia.admin@escapa.com",
                "hash", UserRole.ADMIN, LocalDateTime.now(), "Turismo");
        userRepository.saveAndFlush(admin);
        em.clear();

        final Number adminRows = (Number) em.getEntityManager()
                .createNativeQuery("SELECT count(*) FROM admins WHERE user_id = :id")
                .setParameter("id", admin.getId())
                .getSingleResult();
        final UserEntity reloaded = userRepository.findById(admin.getId()).orElseThrow();

        assertEquals(1L, adminRows.longValue());
        assertInstanceOf(AdminEntity.class, reloaded);
    }

    @Test
    void savingARegularUserShouldWriteTheRegularUsersRow() {
        final RegularUserEntity student = newStudent("aluno.row@email.com");
        userRepository.saveAndFlush(student);

        final Number rows = (Number) em.getEntityManager()
                .createNativeQuery("SELECT count(*) FROM regular_users WHERE user_id = :id")
                .setParameter("id", student.getId())
                .getSingleResult();

        assertEquals(1L, rows.longValue());
    }

    @Test
    void shouldReportWhetherEmailIsAlreadyTaken() {
        userRepository.saveAndFlush(newStudent("joao.exists@email.com"));

        assertTrue(userRepository.existsByEmail("joao.exists@email.com"));
        assertFalse(userRepository.existsByEmail("desconhecido@email.com"));
    }

    @Test
    void databaseShouldRejectDuplicateEmailAsLastLineOfDefense() {
        userRepository.saveAndFlush(newStudent("ana.unique@email.com"));

        assertThrows(DataIntegrityViolationException.class,
                () -> userRepository.saveAndFlush(newStudent("ana.unique@email.com")));
    }

    private static RegularUserEntity newStudent(String email) {
        return new RegularUserEntity(UUID.randomUUID(), "Maria Persist", email, "hash", UserRole.STUDENT,
                LocalDateTime.now(), null, null);
    }
}

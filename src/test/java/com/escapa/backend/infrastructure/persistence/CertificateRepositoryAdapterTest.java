package com.escapa.backend.infrastructure.persistence;

import com.escapa.backend.application.model.CertificateRecord;
import com.escapa.backend.application.port.CertificateRepositoryPort;
import com.escapa.backend.application.port.UserRepositoryPort;
import com.escapa.backend.domain.entity.User;
import com.escapa.backend.infrastructure.persistence.entity.CourseEntity;
import com.escapa.backend.infrastructure.persistence.entity.UserCourseEntity;
import com.escapa.backend.infrastructure.persistence.entity.UserCourseId;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CertificateRepositoryAdapterTest extends PostgresIntegrationTest {

    @Autowired
    private CertificateRepositoryPort certificateRepositoryPort;

    @Autowired
    private CourseJpaRepository courseJpaRepository;

    @Autowired
    private UserRepositoryPort userRepositoryPort;

    @Autowired
    private UserJpaRepository userJpaRepository;

    @Autowired
    private UserCourseJpaRepository userCourseJpaRepository;

    private CourseEntity givenCourse(int durationMinutes) {
        final CourseEntity course = new CourseEntity();
        course.setTitle("Marketing Digital para Hospitalidade");
        course.setDurationTime(durationMinutes);
        return courseJpaRepository.save(course);
    }

    private UUID givenUser(String name) {
        final String email = "aluno-" + UUID.randomUUID() + "@email.com";
        return userRepositoryPort.save(new User(name, email, "hash", "STUDENT")).getId();
    }

    private void givenEnrollment(
            UUID userId, CourseEntity course, String code, boolean issued, LocalDate conclusionDate) {
        final UserCourseEntity enrollment = new UserCourseEntity();
        enrollment.setId(new UserCourseId(userId, course.getId()));
        enrollment.setUser(userJpaRepository.getReferenceById(userId));
        enrollment.setCourse(course);
        enrollment.setCertificateCode(code);
        enrollment.setCertificateIssued(issued);
        enrollment.setConclusionDate(conclusionDate);
        userCourseJpaRepository.save(enrollment);
    }

    @Test
    void shouldFindAnIssuedCertificateByVerificationCode() {
        final CourseEntity course = givenCourse(960);
        final UUID userId = givenUser("Jorge Amado");
        givenEnrollment(userId, course, "ESC-2026-MKT-0001", true, LocalDate.of(2026, 8, 21));

        final Optional<CertificateRecord> found = certificateRepositoryPort.findByVerificationCode("ESC-2026-MKT-0001");

        assertTrue(found.isPresent());
        assertEquals(userId, found.get().userId());
        assertEquals(course.getId(), found.get().courseId());
        assertEquals("Jorge Amado", found.get().studentName());
        assertEquals("Marketing Digital para Hospitalidade", found.get().courseTitle());
        assertEquals(960, found.get().workloadMinutes());
        assertEquals(LocalDate.of(2026, 8, 21), found.get().conclusionDate());
        assertEquals("ESC-2026-MKT-0001", found.get().verificationCode());
        assertNull(found.get().cachedPdf());
    }

    @Test
    void shouldNotFindAnUnknownVerificationCode() {
        assertTrue(certificateRepositoryPort.findByVerificationCode("DOES-NOT-EXIST").isEmpty());
    }

    @Test
    void shouldNotFindACertificateThatWasNotIssuedYet() {
        final CourseEntity course = givenCourse(960);
        final UUID userId = givenUser("Aluno Em Andamento");
        // Sem certificado emitido, certificate_code normalmente ficaria nulo; o teste
        // forca um valor para provar que o filtro por certificate_issued e' respeitado.
        givenEnrollment(userId, course, "ESC-NOT-ISSUED", false, null);

        assertTrue(certificateRepositoryPort.findByVerificationCode("ESC-NOT-ISSUED").isEmpty());
    }

    @Test
    void shouldSaveAndReuseTheCachedPdf() {
        final CourseEntity course = givenCourse(960);
        final UUID userId = givenUser("Jorge Amado");
        givenEnrollment(userId, course, "ESC-2026-MKT-0002", true, LocalDate.of(2026, 8, 21));
        final byte[] pdf = "%PDF-1.4 fake content".getBytes();

        certificateRepositoryPort.saveCachedPdf(userId, course.getId(), pdf);

        final Optional<CertificateRecord> found = certificateRepositoryPort.findByVerificationCode("ESC-2026-MKT-0002");
        assertTrue(found.isPresent());
        assertArrayEquals(pdf, found.get().cachedPdf());
    }
}

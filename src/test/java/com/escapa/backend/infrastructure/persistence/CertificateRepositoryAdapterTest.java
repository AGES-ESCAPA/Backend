package com.escapa.backend.infrastructure.persistence;

import com.escapa.backend.application.model.CertificateDetails;
import com.escapa.backend.application.model.CertificateRecord;
import com.escapa.backend.application.port.CertificateRepositoryPort;
import com.escapa.backend.application.port.UserRepositoryPort;
import com.escapa.backend.domain.entity.User;
import com.escapa.backend.infrastructure.persistence.entity.CourseEntity;
import com.escapa.backend.infrastructure.persistence.entity.UserCourseEntity;
import com.escapa.backend.infrastructure.persistence.entity.UserCourseId;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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

    // admins e course_reviews nao tem repositorio JPA: o teste grava direto nas tabelas.
    @Autowired
    private JdbcTemplate jdbcTemplate;

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

    private UUID givenAdmin(String name, String avatarUrl) {
        final UUID adminId = givenUser(name);
        jdbcTemplate.update("insert into admins (user_id, avatar_url) values (?, ?)", adminId, avatarUrl);
        return adminId;
    }

    private CourseEntity givenFullCourse(UUID instructorId) {
        final CourseEntity course = new CourseEntity();
        course.setTitle("Marketing Digital para Hospitalidade");
        course.setDescription("Estrategias de marketing digital para hoteis");
        course.setCategory("Marketing");
        course.setLevel("Intermediario");
        course.setThumbnailUrl("https://cdn.escapa.com.br/courses/102/thumb.jpg");
        course.setDurationTime(960);
        course.setPrice(117.0);
        final CourseEntity saved = courseJpaRepository.save(course);
        jdbcTemplate.update("update courses set instructor_id = ? where id = ?", instructorId, saved.getId());
        return saved;
    }

    private void givenReview(CourseEntity course, int rating) {
        jdbcTemplate.update(
                "insert into course_reviews (id, course_id, user_id, rating) values (?, ?, ?, ?)",
                UUID.randomUUID(), course.getId(), givenUser("Avaliador"), rating);
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

    @Test
    void shouldFindTheCertificateScreenDataInASingleQuery() {
        final UUID instructorId = givenAdmin("Thamiris Magalhaes", "https://cdn.escapa.com.br/admins/1.jpg");
        final CourseEntity course = givenFullCourse(instructorId);
        final UUID userId = givenUser("Jorge Amado");
        givenEnrollment(userId, course, "ESC-2026-MKT-0100", true, LocalDate.of(2026, 8, 21));
        givenReview(course, 5);
        givenReview(course, 4);

        final Optional<CertificateDetails> found =
                certificateRepositoryPort.findDetailsByVerificationCode("ESC-2026-MKT-0100");

        assertTrue(found.isPresent());
        final CertificateDetails details = found.get();
        assertEquals(LocalDate.of(2026, 8, 21), details.conclusionDate());
        assertEquals(960, details.workloadMinutes());
        assertEquals("ESC-2026-MKT-0100", details.verificationCode());

        assertEquals("Jorge Amado", details.student().name());
        // Aluno comum nao tem avatar no modelo (avatar_url so existe em admins).
        assertNull(details.student().avatarUrl());
        assertTrue(details.student().verified());

        final CertificateDetails.Course courseDetails = details.course();
        assertEquals(course.getId(), courseDetails.id());
        assertEquals("Marketing Digital para Hospitalidade", courseDetails.title());
        assertEquals("Estrategias de marketing digital para hoteis", courseDetails.description());
        assertEquals("Marketing", courseDetails.category());
        assertEquals("Intermediario", courseDetails.level());
        assertEquals("https://cdn.escapa.com.br/courses/102/thumb.jpg", courseDetails.thumbnailUrl());
        assertEquals(960, courseDetails.durationTime());
        assertEquals(0, courseDetails.lessonsCount());
        assertEquals(4.5, courseDetails.rating());
        assertEquals(2, courseDetails.reviewsCount());
        assertEquals("Thamiris Magalhaes", courseDetails.instructor());
        assertEquals(117.0, courseDetails.price());
    }

    @Test
    void shouldReturnTheAvatarWhenTheStudentHasAnAdminProfile() {
        final CourseEntity course = givenCourse(960);
        final UUID adminId = givenAdmin("Admin Aluno", "https://cdn.escapa.com.br/admins/2.jpg");
        givenEnrollment(adminId, course, "ESC-2026-MKT-0101", true, LocalDate.of(2026, 8, 21));

        final CertificateDetails details =
                certificateRepositoryPort.findDetailsByVerificationCode("ESC-2026-MKT-0101").orElseThrow();

        assertEquals("https://cdn.escapa.com.br/admins/2.jpg", details.student().avatarUrl());
    }

    @Test
    void shouldReturnNoRatingAndNoInstructorWhenTheCourseHasNeither() {
        final CourseEntity course = givenCourse(960);
        final UUID userId = givenUser("Jorge Amado");
        givenEnrollment(userId, course, "ESC-2026-MKT-0102", true, LocalDate.of(2026, 8, 21));

        final CertificateDetails details =
                certificateRepositoryPort.findDetailsByVerificationCode("ESC-2026-MKT-0102").orElseThrow();

        assertNull(details.course().rating());
        assertEquals(0, details.course().reviewsCount());
        assertNull(details.course().instructor());
    }

    @Test
    void shouldNotMarkAnInactiveStudentAsVerified() {
        final CourseEntity course = givenCourse(960);
        final UUID userId = givenUser("Aluno Inativo");
        jdbcTemplate.update("update users set status = 'INACTIVE' where id = ?", userId);
        givenEnrollment(userId, course, "ESC-2026-MKT-0103", true, LocalDate.of(2026, 8, 21));

        final CertificateDetails details =
                certificateRepositoryPort.findDetailsByVerificationCode("ESC-2026-MKT-0103").orElseThrow();

        assertFalse(details.student().verified());
    }

    @Test
    void shouldNotFindScreenDataForUnknownOrNotIssuedCertificates() {
        final CourseEntity course = givenCourse(960);
        final UUID userId = givenUser("Aluno Em Andamento");
        givenEnrollment(userId, course, "ESC-DETAILS-NOT-ISSUED", false, null);

        assertTrue(certificateRepositoryPort.findDetailsByVerificationCode("DOES-NOT-EXIST").isEmpty());
        assertTrue(certificateRepositoryPort.findDetailsByVerificationCode("ESC-DETAILS-NOT-ISSUED").isEmpty());
    }
}

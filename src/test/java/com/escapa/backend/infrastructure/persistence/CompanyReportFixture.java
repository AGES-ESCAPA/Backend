package com.escapa.backend.infrastructure.persistence;

import org.springframework.jdbc.core.JdbcTemplate;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/** Dados dos testes do relatório de progresso da equipe (US-35): empresas, colaboradores, cursos e aulas. */
final class CompanyReportFixture {

    private final JdbcTemplate jdbc;
    private int moduleOrder;

    CompanyReportFixture(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    UUID company(String name) {
        final UUID id = UUID.randomUUID();
        jdbc.update("insert into users (id, name, email, password_hash, role, status) "
                + "values (?, ?, ?, 'hash', 'COMPANY', 'ACTIVE')", id, name, id + "@example.com");
        jdbc.update("insert into company (id, company_name, seats_total) values (?, ?, 10)", id, name);
        return id;
    }

    UUID employee(UUID companyId, String name, String status, String jobTitle) {
        final UUID id = UUID.randomUUID();
        jdbc.update("insert into users (id, name, email, password_hash, role, status) "
                + "values (?, ?, ?, 'hash', 'STUDENT', ?)", id, name, id + "@example.com", status);
        jdbc.update("insert into users_company (user_id, company_id, role) values (?, ?, ?)",
                id, companyId, jobTitle);
        return id;
    }

    UUID course(String title) {
        final UUID id = UUID.randomUUID();
        jdbc.update("insert into courses (id, title, status) values (?, ?, 'PUBLISHED')", id, title);
        return id;
    }

    void ownCourse(UUID companyId, UUID courseId) {
        jdbc.update("insert into company_courses (company_id, course_id, data_inicio, data_expiracao) "
                + "values (?, ?, date '2026-01-01', date '2027-01-01')", companyId, courseId);
    }

    void revokeCompanyCourse(UUID companyId, UUID courseId, LocalDateTime at) {
        jdbc.update("update company_courses set revoked_at = ? where company_id = ? and course_id = ?",
                at, companyId, courseId);
    }

    /** Cria um módulo novo no curso com uma aula de {@code minutes} minutos. */
    UUID lesson(UUID courseId, int minutes) {
        final UUID moduleId = UUID.randomUUID();
        moduleOrder++;
        jdbc.update("insert into modules (id, course_id, title, \"order\") values (?, ?, 'Module', ?)",
                moduleId, courseId, moduleOrder);
        final UUID lessonId = UUID.randomUUID();
        jdbc.update("insert into content (id, module_id, title, type, duration_minutes, \"order\") "
                + "values (?, ?, 'Lesson', 'VIDEO', ?, 1)", lessonId, moduleId, minutes);
        return lessonId;
    }

    void completed(UUID userId, UUID lessonId, LocalDateTime at) {
        jdbc.update("insert into user_content_progress (user_id, content_id, completed_at) values (?, ?, ?)",
                userId, lessonId, at);
    }

    void enrollment(UUID userId, UUID courseId, LocalDate start, Integer progress,
            LocalDate conclusion, LocalDateTime lastAccess) {
        jdbc.update("insert into user_courses (user_id, course_id, dt_inicio, progress, conclusion_date, "
                + "last_access_date) values (?, ?, ?, ?, ?, ?)",
                userId, courseId, start, progress, conclusion, lastAccess);
    }

    void revokeEnrollment(UUID userId, UUID courseId, LocalDateTime at) {
        jdbc.update("update user_courses set revoked_at = ? where user_id = ? and course_id = ?",
                at, userId, courseId);
    }

    void certificate(UUID userId, UUID courseId, LocalDateTime issuedAt) {
        jdbc.update("update user_courses set certificate_issued = true, certificate_issued_at = ? "
                + "where user_id = ? and course_id = ?", issuedAt, userId, courseId);
    }
}

package com.escapa.backend.infrastructure.persistence;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Testa a V13 (unifica deadline e access_duration_days) com dados de verdade: aplica as migrations
 * até a V12, cria cursos no formato antigo e roda a V13. Usa um schema próprio no Postgres
 * compartilhado, para não mexer nas tabelas dos outros testes.
 */
class CourseAccessDurationMigrationTest {

    private static final String BEFORE_UNIFY = "12";

    private String schema;
    private DriverManagerDataSource dataSource;
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void migrateUntilBeforeTheUnification() {
        schema = "mig_" + UUID.randomUUID().toString().replace("-", "");
        final String baseUrl = PostgresIntegrationTest.POSTGRES.getJdbcUrl();
        final String separator = baseUrl.contains("?") ? "&" : "?";
        dataSource = new DriverManagerDataSource(
                baseUrl + separator + "currentSchema=" + schema,
                PostgresIntegrationTest.POSTGRES.getUsername(),
                PostgresIntegrationTest.POSTGRES.getPassword());
        jdbcTemplate = new JdbcTemplate(dataSource);

        flyway(BEFORE_UNIFY).migrate();
    }

    @AfterEach
    void dropTheTemporarySchema() {
        jdbcTemplate.execute("drop schema if exists " + schema + " cascade");
    }

    private Flyway flyway(String target) {
        return Flyway.configure()
                .dataSource(dataSource)
                .schemas(schema)
                .defaultSchema(schema)
                .locations("classpath:db/migration")
                .target(target)
                .load();
    }

    private void migrateToLatest() {
        flyway("latest").migrate();
    }

    private UUID givenCourse(Integer deadline, Integer accessDurationDays) {
        final UUID id = UUID.randomUUID();
        jdbcTemplate.update(
                "insert into courses (id, title, deadline, access_duration_days) values (?, 'Curso', ?, ?)",
                id, deadline, accessDurationDays);
        return id;
    }

    private Integer deadlineOf(UUID courseId) {
        return jdbcTemplate.queryForObject(
                "select deadline from courses where id = ?", Integer.class, courseId);
    }

    @Test
    void shouldCopyTheOldAccessDurationToCoursesThatOnlyHadIt() {
        final UUID onlyOldColumn = givenCourse(null, 365);

        migrateToLatest();

        assertEquals(365, deadlineOf(onlyOldColumn));
    }

    @Test
    void shouldKeepTheDeadlineWhenTheCourseHadBothColumns() {
        final UUID both = givenCourse(30, 90);

        migrateToLatest();

        assertEquals(30, deadlineOf(both));
    }

    @Test
    void shouldKeepTheDeadlineOfCoursesThatOnlyHadIt() {
        final UUID onlyDeadline = givenCourse(180, null);

        migrateToLatest();

        assertEquals(180, deadlineOf(onlyDeadline));
    }

    @Test
    void shouldLeaveTheDeadlineEmptyWhenNeitherColumnHadAValue() {
        final UUID neither = givenCourse(null, null);

        migrateToLatest();

        assertNull(deadlineOf(neither));
    }

    @Test
    void shouldMigrateEveryCourseAtOnce() {
        final UUID a = givenCourse(null, 365);
        final UUID b = givenCourse(30, 90);
        final UUID c = givenCourse(null, 60);
        final UUID d = givenCourse(null, null);

        migrateToLatest();

        assertEquals(List.of(365, 30, 60), List.of(deadlineOf(a), deadlineOf(b), deadlineOf(c)));
        assertNull(deadlineOf(d));
    }

    @Test
    void shouldDropTheOldColumn() {
        migrateToLatest();

        final List<String> columns = jdbcTemplate.queryForList(
                "select column_name from information_schema.columns"
                        + " where table_schema = ? and table_name = 'courses'",
                String.class, schema);

        assertFalse(columns.contains("access_duration_days"));
        assertEquals(1, columns.stream().filter("deadline"::equals).count());
    }
}

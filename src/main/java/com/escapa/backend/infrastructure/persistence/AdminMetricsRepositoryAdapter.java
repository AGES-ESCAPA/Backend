package com.escapa.backend.infrastructure.persistence;

import com.escapa.backend.application.model.EnrollmentCompletionCounts;
import com.escapa.backend.application.port.AdminMetricsRepositoryPort;
import org.springframework.jdbc.core.simple.JdbcClient;
import java.time.LocalDate;

public class AdminMetricsRepositoryAdapter implements AdminMetricsRepositoryPort {
    private static final String ACTIVE_STUDENTS = """
            select count(distinct u.id)
              from users u
              join user_courses enrollment on enrollment.user_id = u.id
              join courses course on course.id = enrollment.course_id
             where u.role = 'STUDENT' and u.status = 'ACTIVE'
               and enrollment.revoked_at is null and course.status <> 'DRAFT'
               and (enrollment.dt_inicio is null or enrollment.dt_expiracao is null
                    or enrollment.dt_inicio <= enrollment.dt_expiracao)
               and (enrollment.dt_inicio is null or enrollment.dt_inicio <= :endDate)
               and (enrollment.dt_expiracao is null or enrollment.dt_expiracao >= :startDate)
            """;
    private final JdbcClient jdbcClient;

    public AdminMetricsRepositoryAdapter(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    @Override
    public long countActiveStudents(LocalDate date) {
        return countActiveStudentsInPeriod(date, date);
    }

    @Override
    public long countActiveStudentsInPeriod(LocalDate from, LocalDate to) {
        return jdbcClient.sql(ACTIVE_STUDENTS).param("startDate", from).param("endDate", to)
                .query(Long.class).single();
    }

    @Override
    public long countPartnerCompanies() {
        return jdbcClient.sql("select count(*) from users where role = 'COMPANY' and status = 'ACTIVE'")
                .query(Long.class).single();
    }

    @Override
    public EnrollmentCompletionCounts countHistoricalCompletion() {
        return jdbcClient.sql("""
                        select count(*) as eligible,
                               count(*) filter (where enrollment.conclusion_date is not null
                                                or enrollment.progress = 100) as completed
                          from user_courses enrollment
                          join courses course on course.id = enrollment.course_id
                         where enrollment.revoked_at is null and course.status <> 'DRAFT'
                        """)
                .query((rs, rowNum) -> new EnrollmentCompletionCounts(
                        rs.getLong("eligible"), rs.getLong("completed")))
                .single();
    }
}

package com.escapa.backend.infrastructure.persistence;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.springframework.jdbc.core.simple.JdbcClient;

import com.escapa.backend.application.model.CompanyEngagement;
import com.escapa.backend.application.model.EnrollmentCompletionCounts;
import com.escapa.backend.application.port.AdminMetricsRepositoryPort;

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
    public long countPartnerCompaniesCreatedBefore(LocalDate startOfCurrentMonth) {
        return jdbcClient.sql("""
                select count(*)
                  from users
                 where role = 'COMPANY' and status = 'ACTIVE'
                   and created_at < :startOfCurrentMonth
                """)
                .param("startOfCurrentMonth", startOfCurrentMonth)
                .query(Long.class).single();
    }

    @Override
    public List<CompanyEngagement> findTopEngagedCompanies(int limit) {
        return jdbcClient.sql("""
                select company.id as company_id,
                       company.company_name as name,
                       count(*) as enrollments
                  from company
                  join users account on account.id = company.id
                  join users_company link on link.company_id = company.id
                  join user_courses enrollment on enrollment.user_id = link.user_id
                  join courses course on course.id = enrollment.course_id
                 where account.role = 'COMPANY' and account.status = 'ACTIVE'
                   and enrollment.revoked_at is null and course.status <> 'DRAFT'
                 group by company.id, company.company_name
                 order by enrollments desc, company.company_name asc, company.id asc
                 limit :resultLimit
                """)
                .param("resultLimit", limit)
                .query((rs, rowNum) -> new CompanyEngagement(
                        rs.getObject("company_id", UUID.class),
                        rs.getString("name"),
                        rs.getLong("enrollments")))
                .list();
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

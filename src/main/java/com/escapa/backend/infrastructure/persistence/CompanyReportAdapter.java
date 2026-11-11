package com.escapa.backend.infrastructure.persistence;

import com.escapa.backend.application.dto.PageResult;
import com.escapa.backend.application.model.CompanyEmployeeProgress;
import com.escapa.backend.application.model.CompanyReportFigures;
import com.escapa.backend.application.model.CompanyReportFigures.JobTitleFigures;
import com.escapa.backend.application.model.CompanyReportFigures.MonthFigures;
import com.escapa.backend.application.model.CompanyReportPeriod;
import com.escapa.backend.application.model.EmployeeProgressStatus;
import com.escapa.backend.application.port.CompanyReportRepositoryPort;
import org.springframework.jdbc.core.simple.JdbcClient;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * Leituras do relatório de progresso da equipe (US-35). Toda consulta parte de {@code users_company}
 * filtrado pela empresa, então um colaborador ou curso de outra empresa nunca entra no resultado.
 */
public class CompanyReportAdapter implements CompanyReportRepositoryPort {

    /** Mesma ordenação Unicode da busca de colaboradores (migration V19). */
    private static final String NAME_KEY = "lower(u.name collate escapa_unicode)";

    /** Mesma regra de "concluída" do restante do sistema: data de conclusão ou 100% de progresso. */
    private static final String COMPLETED =
            "(enrollment.conclusion_date is not null or enrollment.progress = 100)";

    private static final String NOT_COMPLETED =
            "(enrollment.conclusion_date is null and (enrollment.progress is null or enrollment.progress < 100))";

    /**
     * Matrículas não revogadas dos colaboradores da empresa, nos cursos que ela comprou e ainda tem
     * (um curso revogado pelo cancelamento do pedido não conta). Mesmo escopo do "assigned_courses" da US-30.
     */
    private static final String ENROLLMENTS_FROM = """
              from users_company link
              join users u on u.id = link.user_id
              join user_courses enrollment
                on enrollment.user_id = u.id and enrollment.revoked_at is null
              join company_courses owned
                on owned.course_id = enrollment.course_id and owned.company_id = link.company_id
               and owned.revoked_at is null
             where link.company_id = :companyId
            """;

    /** Aulas concluídas pelos colaboradores ACTIVE, em matrículas válidas de cursos da empresa. */
    private static final String LESSONS_FROM = """
              from users_company link
              join users u on u.id = link.user_id and u.status = 'ACTIVE'
              join user_content_progress progress on progress.user_id = u.id
              join content lesson on lesson.id = progress.content_id
              join modules course_module on course_module.id = lesson.module_id
              join user_courses enrollment
                on enrollment.user_id = u.id and enrollment.course_id = course_module.course_id
               and enrollment.revoked_at is null
              join company_courses owned
                on owned.course_id = course_module.course_id and owned.company_id = link.company_id
               and owned.revoked_at is null
             where link.company_id = :companyId
            """;

    private final JdbcClient jdbcClient;

    public CompanyReportAdapter(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    @Override
    public boolean existsCompany(UUID companyId) {
        return jdbcClient.sql("select exists (select 1 from company where id = :companyId)")
                .param("companyId", companyId)
                .query(Boolean.class)
                .single();
    }

    @Override
    public CompanyReportFigures loadFigures(UUID companyId, CompanyReportPeriod period) {
        final EnrollmentCounts counts = enrollmentCounts(companyId, period);
        return new CompanyReportFigures(
                activeEmployees(companyId),
                counts.enrollments(),
                counts.completed(),
                completedMinutes(companyId),
                counts.certificatesThisQuarter(),
                counts.activeLastWeek(),
                counts.enrollmentsAtPreviousMonthEnd(),
                counts.completedAtPreviousMonthEnd(),
                jobTitleFigures(companyId),
                monthlyMinutes(companyId, period));
    }

    private EnrollmentCounts enrollmentCounts(UUID companyId, CompanyReportPeriod period) {
        return jdbcClient.sql("""
                        select count(*) as enrollments,
                               count(*) filter (where """ + COMPLETED + """
                               ) as completed,
                               count(*) filter (where enrollment.certificate_issued
                                                  and enrollment.certificate_issued_at >= :quarterStart) as certificates,
                               count(distinct u.id) filter (where u.status = 'ACTIVE'
                                                              and enrollment.last_access_date >= :weekStart) as active_last_week,
                               count(*) filter (where enrollment.dt_inicio is null
                                                   or enrollment.dt_inicio <= :previousMonthEnd) as previous_enrollments,
                               count(*) filter (where (enrollment.dt_inicio is null
                                                       or enrollment.dt_inicio <= :previousMonthEnd)
                                                  and enrollment.conclusion_date <= :previousMonthEnd) as previous_completed
                        """ + ENROLLMENTS_FROM)
                .param("companyId", companyId)
                .param("quarterStart", period.quarterStart())
                .param("weekStart", period.weekStart())
                .param("previousMonthEnd", period.previousMonthEnd())
                .query((rs, rowNum) -> new EnrollmentCounts(
                        rs.getLong("enrollments"),
                        rs.getLong("completed"),
                        rs.getLong("certificates"),
                        rs.getLong("active_last_week"),
                        rs.getLong("previous_enrollments"),
                        rs.getLong("previous_completed")))
                .single();
    }

    private long activeEmployees(UUID companyId) {
        return jdbcClient.sql("""
                        select count(*)
                          from users_company link
                          join users u on u.id = link.user_id
                         where link.company_id = :companyId and u.status = 'ACTIVE'
                        """)
                .param("companyId", companyId)
                .query(Long.class)
                .single();
    }

    private long completedMinutes(UUID companyId) {
        return jdbcClient.sql("select coalesce(sum(coalesce(lesson.duration_minutes, 0)), 0)" + LESSONS_FROM)
                .param("companyId", companyId)
                .query(Long.class)
                .single();
    }

    private List<JobTitleFigures> jobTitleFigures(UUID companyId) {
        return jdbcClient.sql("""
                        select link.role as job_title,
                               count(*) as enrollments,
                               count(*) filter (where """ + COMPLETED + """
                               ) as completed
                        """ + ENROLLMENTS_FROM + " group by link.role")
                .param("companyId", companyId)
                .query((rs, rowNum) -> new JobTitleFigures(
                        rs.getString("job_title"), rs.getLong("enrollments"), rs.getLong("completed")))
                .list();
    }

    private List<MonthFigures> monthlyMinutes(UUID companyId, CompanyReportPeriod period) {
        return jdbcClient.sql("""
                        select to_char(progress.completed_at, 'YYYY-MM') as lesson_month,
                               sum(coalesce(lesson.duration_minutes, 0)) as minutes
                        """ + LESSONS_FROM + """
                           and progress.completed_at >= :monthlyFrom
                         group by 1
                        """)
                .param("companyId", companyId)
                .param("monthlyFrom", period.monthlyFrom())
                .query((rs, rowNum) -> new MonthFigures(
                        YearMonth.parse(rs.getString("lesson_month")), rs.getLong("minutes")))
                .list();
    }

    @Override
    public PageResult<CompanyEmployeeProgress> findEmployeeProgress(
            UUID companyId, String search, EmployeeProgressStatus status, int page, int size) {
        final String pattern = search == null ? null : "%" + escapeLike(search.toLowerCase(Locale.ROOT)) + "%";
        final String filter = filters(pattern, status);

        final long total = bind(jdbcClient.sql("select count(*)" + listFrom() + filter), companyId, pattern)
                .query(Long.class)
                .single();

        final List<CompanyEmployeeProgress> content = bind(jdbcClient.sql("""
                        select u.id as user_id, u.name, u.avatar_url, course.id as course_id, course.title,
                               case when enrollment.conclusion_date is not null then 100
                                    else coalesce(enrollment.progress, 0) end as progress,
                               enrollment.last_access_date, enrollment.certificate_issued
                        """ + listFrom() + filter + """
                         order by enrollment.last_access_date desc nulls last, """ + NAME_KEY + """
                               , u.id, course.id
                         limit :limit offset :offset
                        """), companyId, pattern)
                .param("limit", size)
                .param("offset", (long) page * size)
                .query((rs, rowNum) -> toProgress(rs))
                .list();

        final int totalPages = (int) ((total + size - 1) / size);
        return new PageResult<>(content, page, size, total, totalPages);
    }

    private static String listFrom() {
        return ENROLLMENTS_FROM.replace(
                "where link.company_id = :companyId",
                "join courses course on course.id = enrollment.course_id\n where link.company_id = :companyId");
    }

    private static String filters(String pattern, EmployeeProgressStatus status) {
        final String byName = pattern == null ? "" : " and " + NAME_KEY + " like :pattern escape '\\'\n";
        if (status == null) {
            return byName;
        }
        return byName + " and " + (status == EmployeeProgressStatus.COMPLETED ? COMPLETED : NOT_COMPLETED) + "\n";
    }

    private static CompanyEmployeeProgress toProgress(java.sql.ResultSet rs) throws java.sql.SQLException {
        final Timestamp lastAccess = rs.getTimestamp("last_access_date");
        final LocalDateTime lastAccessAt = lastAccess == null ? null : lastAccess.toLocalDateTime();
        return new CompanyEmployeeProgress(
                rs.getObject("user_id", UUID.class),
                rs.getString("name"),
                rs.getString("avatar_url"),
                rs.getObject("course_id", UUID.class),
                rs.getString("title"),
                rs.getInt("progress"),
                lastAccessAt,
                rs.getBoolean("certificate_issued"));
    }

    private static JdbcClient.StatementSpec bind(JdbcClient.StatementSpec statement, UUID companyId, String pattern) {
        final JdbcClient.StatementSpec bound = statement.param("companyId", companyId);
        return pattern == null ? bound : bound.param("pattern", pattern);
    }

    /** Escapa os curingas do LIKE, para "50%" ou "a_b" serem procurados como texto. */
    private static String escapeLike(String text) {
        return text.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }

    private record EnrollmentCounts(
            long enrollments,
            long completed,
            long certificatesThisQuarter,
            long activeLastWeek,
            long enrollmentsAtPreviousMonthEnd,
            long completedAtPreviousMonthEnd
    ) {
    }
}

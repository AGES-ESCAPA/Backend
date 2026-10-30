package com.escapa.backend.infrastructure.persistence;

import com.escapa.backend.application.dto.PageResult;
import com.escapa.backend.application.model.CompanyEmployee;
import com.escapa.backend.application.model.CompanyEmployeeCounts;
import com.escapa.backend.application.port.CompanyEmployeeRepositoryPort;
import com.escapa.backend.domain.user.UserStatus;
import org.springframework.jdbc.core.simple.JdbcClient;

import java.util.List;
import java.util.UUID;

/**
 * Colaboradores de uma empresa lidos de {@code users_company}. Toda consulta filtra por
 * {@code company_id}, então um colaborador de outra empresa nunca entra no resultado.
 */
public class CompanyEmployeeAdapter implements CompanyEmployeeRepositoryPort {

    /**
     * A busca e a ordem usam a ordenação Unicode (ICU) do Postgres, "und-x-icu". Sem ela, a ordem
     * depende da configuração do banco e nomes com acento ("Ágata", "Érica") vão parar depois do "Z".
     */
    private static final String SEARCH_FILTER = """
             and (lower(u.name collate "und-x-icu") like :pattern escape '\\'
                  or lower(u.email collate "und-x-icu") like :pattern escape '\\')
            """;

    /**
     * Matrículas do colaborador que não foram revogadas, nos cursos que a empresa comprou e ainda
     * tem (um curso da empresa revogado pelo cancelamento do pedido não conta).
     */
    private static final String ASSIGNED_COURSES = """
            (select count(*)
               from user_courses enrollment
               join company_courses owned
                 on owned.course_id = enrollment.course_id and owned.company_id = link.company_id
              where enrollment.user_id = u.id
                and enrollment.revoked_at is null
                and owned.revoked_at is null)
            """;

    private final JdbcClient jdbcClient;

    public CompanyEmployeeAdapter(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    @Override
    public CompanyEmployeeCounts countEmployees(UUID companyId) {
        return jdbcClient.sql("""
                        select count(*) as total,
                               count(*) filter (where u.status = 'ACTIVE') as active
                          from users_company link
                          join users u on u.id = link.user_id
                         where link.company_id = :companyId
                        """)
                .param("companyId", companyId)
                .query((rs, rowNum) -> new CompanyEmployeeCounts(rs.getInt("total"), rs.getInt("active")))
                .single();
    }

    @Override
    public PageResult<CompanyEmployee> findEmployees(UUID companyId, String search, int page, int size) {
        final boolean filtered = search != null;
        final String pattern = filtered ? "%" + escapeLike(search.toLowerCase()) + "%" : null;
        final String filter = filtered ? SEARCH_FILTER : "";

        final long total = bind(jdbcClient.sql("""
                        select count(*)
                          from users_company link
                          join users u on u.id = link.user_id
                         where link.company_id = :companyId
                        """ + filter), companyId, pattern)
                .query(Long.class)
                .single();

        final List<CompanyEmployee> content = bind(jdbcClient.sql("""
                        select u.id, u.name, u.email, link.role as job_title, u.status, u.avatar_url,
                        """ + ASSIGNED_COURSES + """
                               as assigned_courses
                          from users_company link
                          join users u on u.id = link.user_id
                         where link.company_id = :companyId
                        """ + filter + """
                         order by lower(u.name collate "und-x-icu"), u.id
                         limit :limit offset :offset
                        """), companyId, pattern)
                .param("limit", size)
                .param("offset", (long) page * size)
                .query((rs, rowNum) -> new CompanyEmployee(
                        rs.getObject("id", UUID.class),
                        rs.getString("name"),
                        rs.getString("email"),
                        rs.getString("job_title"),
                        UserStatus.valueOf(rs.getString("status")),
                        rs.getInt("assigned_courses"),
                        rs.getString("avatar_url")))
                .list();

        final int totalPages = (int) ((total + size - 1) / size);
        return new PageResult<>(content, page, size, total, totalPages);
    }

    private static JdbcClient.StatementSpec bind(JdbcClient.StatementSpec statement, UUID companyId, String pattern) {
        final JdbcClient.StatementSpec bound = statement.param("companyId", companyId);
        return pattern == null ? bound : bound.param("pattern", pattern);
    }

    /** Escapa os curingas do LIKE, para "50%" ou "a_b" serem procurados como texto. */
    private static String escapeLike(String text) {
        return text.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }
}

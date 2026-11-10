package com.escapa.backend.infrastructure.persistence;

import com.escapa.backend.application.port.CompanyEmployeeUnlinkPort;
import org.springframework.jdbc.core.simple.JdbcClient;

import java.time.LocalDateTime;
import java.util.UUID;

public class CompanyEmployeeUnlinkAdapter implements CompanyEmployeeUnlinkPort {

    private final JdbcClient jdbcClient;

    public CompanyEmployeeUnlinkAdapter(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    @Override
    public boolean removeLink(UUID companyId, UUID employeeId) {
        return jdbcClient.sql("delete from users_company where company_id = :companyId and user_id = :employeeId")
                .param("companyId", companyId)
                .param("employeeId", employeeId)
                .update() > 0;
    }

    @Override
    public int revokeCompanyCourseEnrollments(UUID companyId, UUID employeeId, LocalDateTime revokedAt) {
        return jdbcClient.sql("""
                update user_courses enrollment
                   set revoked_at = :revokedAt
                 where enrollment.user_id = :employeeId
                   and enrollment.revoked_at is null
                   and enrollment.course_id in (
                       select owned.course_id from company_courses owned where owned.company_id = :companyId)
                   and not exists (
                       select 1 from orders purchase
                        where purchase.buyer_user_id = enrollment.user_id
                          and purchase.course_id = enrollment.course_id
                          and purchase.type = 'INDIVIDUAL'
                          and purchase.item_type = 'COURSE'
                          and purchase.status = 'APPROVED')
                """)
                .param("revokedAt", revokedAt)
                .param("employeeId", employeeId)
                .param("companyId", companyId)
                .update();
    }

    @Override
    public int invalidatePendingInvites(UUID companyId, UUID employeeId, LocalDateTime now) {
        return jdbcClient.sql("""
                update account_invites
                   set expires_at = :now
                 where user_id = :employeeId
                   and company_id = :companyId
                   and used_at is null
                   and expires_at > :now
                """)
                .param("now", now)
                .param("employeeId", employeeId)
                .param("companyId", companyId)
                .update();
    }
}

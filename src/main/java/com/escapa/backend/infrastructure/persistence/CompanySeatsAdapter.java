package com.escapa.backend.infrastructure.persistence;

import com.escapa.backend.application.port.CompanySeatsPort;
import org.springframework.jdbc.core.simple.JdbcClient;

import java.util.UUID;

public class CompanySeatsAdapter implements CompanySeatsPort {

    private final JdbcClient jdbcClient;

    public CompanySeatsAdapter(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    @Override
    public int findSeatsTotal(UUID companyId) {
        return jdbcClient.sql("select seats_total from company where id = :id")
                .param("id", companyId)
                .query(Integer.class)
                .single();
    }

    @Override
    public int countSeatsInUse(UUID companyId) {
        return jdbcClient.sql("select count(*) from users_company where company_id = :id")
                .param("id", companyId)
                .query(Integer.class)
                .single();
    }

    @Override
    public void updateSeatsTotal(UUID companyId, int seatsTotal) {
        jdbcClient.sql("update company set seats_total = :seatsTotal where id = :id")
                .param("seatsTotal", seatsTotal)
                .param("id", companyId)
                .update();
    }
}

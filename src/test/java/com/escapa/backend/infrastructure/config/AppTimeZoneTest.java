package com.escapa.backend.infrastructure.config;

import com.escapa.backend.infrastructure.persistence.PostgresIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.Clock;
import java.time.ZoneId;
import java.util.TimeZone;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AppTimeZoneTest extends PostgresIntegrationTest {

    private static final String EXPECTED_ZONE = "America/Sao_Paulo";

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void shouldUseSaoPauloAsTheDefaultZoneOfTheJvm() {
        assertEquals(EXPECTED_ZONE, TimeZone.getDefault().getID());
        assertEquals(ZoneId.of(EXPECTED_ZONE), Clock.systemDefaultZone().getZone());
    }

    @Test
    void shouldUseTheSameZoneInTheDatabaseSession() {
        final String zone = jdbcTemplate.queryForObject("select current_setting('TimeZone')", String.class);

        assertEquals(EXPECTED_ZONE, zone);
    }
}

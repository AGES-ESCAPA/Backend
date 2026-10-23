package com.escapa.backend.infrastructure.persistence;

import com.escapa.backend.domain.entity.Company;
import com.escapa.backend.domain.entity.User;
import com.escapa.backend.domain.user.UserStatus;
import com.escapa.backend.infrastructure.persistence.entity.CompanyEntity;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class CompanyMapperTest extends PostgresIntegrationTest {

    @Test
    void shouldMapCompanyAndUserFieldsToEntity() {
        final UUID id = UUID.randomUUID();
        final LocalDateTime createdAt = LocalDateTime.of(2025, 1, 2, 3, 4);
        final User user = new User(id, "Company User", "company@example.com", "hash",
                "COMPANY", null, UserStatus.ACTIVE, createdAt);
        final Company company = new Company(id, "Example Ltd", "12345678000199",
                "contact@example.com", "+55 51 99999-0000");

        final CompanyEntity entity = CompanyMapper.toEntity(user, company);

        assertEquals(id, entity.getId());
        assertEquals("Company User", entity.getName());
        assertEquals("company@example.com", entity.getEmail());
        assertEquals("hash", entity.getPasswordHash());
        assertEquals("COMPANY", entity.getRole());
        assertEquals(createdAt, entity.getCreatedAt());
        assertEquals("Example Ltd", entity.getCompanyName());
        assertEquals("12345678000199", entity.getCnpjId());
        assertEquals("contact@example.com", entity.getCompanyEmail());
        assertEquals("+55 51 99999-0000", entity.getPhone());
    }

    @Test
    void shouldMapEntityToCompanyAndReturnNullForNullEntity() {
        final UUID id = UUID.randomUUID();
        final CompanyEntity entity = new CompanyEntity(id, "Company User", "company@example.com",
                "hash", "COMPANY", LocalDateTime.of(2025, 1, 2, 3, 4),
                "Example Ltd", "12345678000199", "contact@example.com", null);
        entity.setPhone("+55 51 99999-0000");

        final Company company = CompanyMapper.toDomain(entity);

        assertEquals(id, company.getId());
        assertEquals("Example Ltd", company.getCompanyName());
        assertEquals("12345678000199", company.getCnpjId());
        assertEquals("contact@example.com", company.getCompanyEmail());
        assertEquals("+55 51 99999-0000", company.getPhone());
        assertNull(CompanyMapper.toDomain(null));
    }
}

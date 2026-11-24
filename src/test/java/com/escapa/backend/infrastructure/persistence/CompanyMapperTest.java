package com.escapa.backend.infrastructure.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.time.LocalDateTime;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.escapa.backend.domain.entity.Company;
import com.escapa.backend.domain.entity.User;
import com.escapa.backend.domain.user.UserTypes;
import com.escapa.backend.infrastructure.persistence.entity.CompanyEntity;

class CompanyMapperTest {

    @Test
    void shouldMapUserAndCompanyToEntityGeneratingCreatedAtWhenMissing() {
        final UUID id = UUID.randomUUID();
        final User user = new User(id, "Ana Responsavel", "contato@vistamar.com", "hash",
                UserTypes.COMPANY, null, null);
        user.setCreatedAt(null);
        final Company company = new Company(
                id, "Pousada Vista Mar LTDA", "12345678000191", "rh@vistamar.com", "51999990001");

        final CompanyEntity entity = CompanyMapper.toEntity(user, company);

        assertEquals(id, entity.getId());
        assertEquals("Ana Responsavel", entity.getName());
        assertEquals("contato@vistamar.com", entity.getEmail());
        assertEquals("hash", entity.getPasswordHash());
        assertEquals(UserTypes.COMPANY, entity.getRole());
        assertEquals("Pousada Vista Mar LTDA", entity.getCompanyName());
        assertEquals("12345678000191", entity.getCnpjId());
        assertEquals("rh@vistamar.com", entity.getCompanyEmail());
        assertEquals("51999990001", entity.getPhone());
        assertNull(entity.getMatricula());
        assertNotNull(entity.getCreatedAt());
    }

    @Test
    void shouldPreserveCreatedAtWhenPresent() {
        final UUID id = UUID.randomUUID();
        final LocalDateTime createdAt = LocalDateTime.of(2026, 6, 15, 10, 0);
        final User user = new User(id, "Ana", "ana@empresa.com", "hash", UserTypes.COMPANY, null, createdAt);
        final Company company = new Company(id, "Empresa", "11222333000144", null, null);

        final CompanyEntity entity = CompanyMapper.toEntity(user, company);

        assertEquals(createdAt, entity.getCreatedAt());
    }

    @Test
    void shouldMapEntityToDomainAndNull() {
        assertNull(CompanyMapper.toDomain(null));

        final UUID id = UUID.randomUUID();
        final CompanyEntity entity = new CompanyEntity(
                id, "Ana", "ana@empresa.com", "hash", UserTypes.COMPANY,
                LocalDateTime.of(2026, 6, 15, 10, 0),
                "Empresa LTDA", "11222333000144", "rh@empresa.com", null);
        entity.setPhone("51999990001");

        final Company domain = CompanyMapper.toDomain(entity);

        assertEquals(id, domain.getId());
        assertEquals("Empresa LTDA", domain.getCompanyName());
        assertEquals("11222333000144", domain.getCnpjId());
        assertEquals("rh@empresa.com", domain.getCompanyEmail());
        assertEquals("51999990001", domain.getPhone());
    }
}

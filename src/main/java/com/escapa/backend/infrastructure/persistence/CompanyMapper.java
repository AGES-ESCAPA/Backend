package com.escapa.backend.infrastructure.persistence;

import java.time.LocalDateTime;

import com.escapa.backend.domain.entity.Company;
import com.escapa.backend.domain.entity.User;
import com.escapa.backend.infrastructure.persistence.entity.CompanyEntity;

public final class CompanyMapper {

    private CompanyMapper() {
    }

    public static CompanyEntity toEntity(User user, Company company) {
        final LocalDateTime createdAt = user.getCreatedAt() != null ? user.getCreatedAt() : LocalDateTime.now();
        final CompanyEntity entity = new CompanyEntity(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getPasswordHash(),
                user.getUserType(),
                createdAt,
                company.getCompanyName(),
                company.getCnpjId(),
                company.getCompanyEmail(),
                null
        );
        entity.setPhone(company.getPhone());
        return entity;
    }

    public static Company toDomain(CompanyEntity entity) {
        if (entity == null) {
            return null;
        }
        return new Company(
                entity.getId(),
                entity.getCompanyName(),
                entity.getCnpjId(),
                entity.getCompanyEmail(),
                entity.getPhone()
        );
    }
}
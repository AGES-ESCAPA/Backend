package com.escapa.backend.infrastructure.persistence;

import com.escapa.backend.application.port.CompanyRepositoryPort;
import com.escapa.backend.domain.entity.Company;
import com.escapa.backend.domain.entity.User;
import com.escapa.backend.infrastructure.persistence.entity.CompanyEntity;

import java.util.Optional;
import java.util.UUID;

public class CompanyRepositoryAdapter implements CompanyRepositoryPort {

    private final CompanyJpaRepository companyJpaRepository;

    public CompanyRepositoryAdapter(CompanyJpaRepository companyJpaRepository) {
        this.companyJpaRepository = companyJpaRepository;
    }

    @Override
    public boolean existsByCnpjId(String cnpjId) {
        return companyJpaRepository.existsByCnpjId(cnpjId);
    }

    @Override
    public Company save(User user, Company company) {
        final CompanyEntity saved = companyJpaRepository.save(CompanyMapper.toEntity(user, company));
        return CompanyMapper.toDomain(saved);
    }

    @Override
    public Optional<Company> findById(UUID id) {
        return companyJpaRepository.findById(id).map(CompanyMapper::toDomain);
    }

    @Override
    public void update(Company company) {
        companyJpaRepository.findById(company.getId()).ifPresent(entity -> {
            entity.setCompanyName(company.getCompanyName());
            entity.setPhone(company.getPhone());
            companyJpaRepository.save(entity);
        });
    }
}
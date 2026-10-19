package com.escapa.backend.application.usecase;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import com.escapa.backend.application.port.CompanyRepositoryPort;
import com.escapa.backend.domain.entity.Company;
import com.escapa.backend.domain.entity.User;

class InMemoryCompanyRepositoryPort implements CompanyRepositoryPort {

    private final InMemoryUserRepositoryPort userRepository;
    private final Map<UUID, Company> companies = new LinkedHashMap<>();

    InMemoryCompanyRepositoryPort(InMemoryUserRepositoryPort userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public boolean existsByCnpjId(String cnpjId) {
        return companies.values().stream().anyMatch(company -> cnpjId.equals(company.getCnpjId()));
    }

    @Override
    public Company save(User user, Company company) {
        userRepository.save(user);
        companies.put(company.getId(), company);
        return company;
    }

    Company find(UUID id) {
        return companies.get(id);
    }

    int count() {
        return companies.size();
    }
}
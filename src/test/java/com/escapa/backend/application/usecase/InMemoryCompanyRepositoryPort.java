package com.escapa.backend.application.usecase;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import com.escapa.backend.application.port.CompanyRepositoryPort;
import com.escapa.backend.domain.entity.Company;
import com.escapa.backend.domain.entity.User;

public class InMemoryCompanyRepositoryPort implements CompanyRepositoryPort {

    private final InMemoryUserRepositoryPort userRepository;
    private final Map<UUID, Company> companies = new LinkedHashMap<>();

    public InMemoryCompanyRepositoryPort(InMemoryUserRepositoryPort userRepository) {
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

    @Override
    public Optional<Company> findById(UUID id) {
        return Optional.ofNullable(companies.get(id)).map(InMemoryCompanyRepositoryPort::copy);
    }

    @Override
    public void update(Company company) {
        final Company stored = companies.get(company.getId());
        if (stored != null) {
            stored.setCompanyName(company.getCompanyName());
            stored.setPhone(company.getPhone());
        }
    }

    Company find(UUID id) {
        return companies.get(id);
    }

    int count() {
        return companies.size();
    }

    private static Company copy(Company company) {
        return new Company(company.getId(), company.getCompanyName(), company.getCnpjId(),
                company.getCompanyEmail(), company.getPhone());
    }
}

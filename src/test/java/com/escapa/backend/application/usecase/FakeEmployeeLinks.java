package com.escapa.backend.application.usecase;

import com.escapa.backend.application.dto.PageResult;
import com.escapa.backend.application.model.CompanyEmployee;
import com.escapa.backend.application.model.CompanyEmployeeCounts;
import com.escapa.backend.application.port.CompanyEmployeeRepositoryPort;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Vínculos colaborador-empresa em memória, só o que o cadastro, o reenvio de convite e a compra usam. */
public final class FakeEmployeeLinks implements CompanyEmployeeRepositoryPort {

    private final Map<UUID, Set<UUID>> links = new LinkedHashMap<>();
    private String lastJobTitle;

    @Override
    public void linkEmployee(UUID companyId, UUID userId, String jobTitle) {
        links.computeIfAbsent(companyId, ignored -> new LinkedHashSet<>()).add(userId);
        lastJobTitle = jobTitle;
    }

    @Override
    public boolean isEmployeeOfCompany(UUID companyId, UUID userId) {
        return links.getOrDefault(companyId, Set.of()).contains(userId);
    }

    @Override
    public List<UUID> findEmployeeIds(UUID companyId) {
        return List.copyOf(links.getOrDefault(companyId, Set.of()));
    }

    String lastJobTitle() {
        return lastJobTitle;
    }

    int count() {
        return links.values().stream().mapToInt(Set::size).sum();
    }

    @Override
    public boolean existsCompany(UUID companyId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public CompanyEmployeeCounts countEmployees(UUID companyId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public PageResult<CompanyEmployee> findEmployees(UUID companyId, String search, int page, int size) {
        throw new UnsupportedOperationException();
    }
}

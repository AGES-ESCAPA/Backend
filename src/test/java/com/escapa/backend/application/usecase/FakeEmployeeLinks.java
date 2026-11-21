package com.escapa.backend.application.usecase;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import com.escapa.backend.application.dto.PageResult;
import com.escapa.backend.application.model.CompanyEmployee;
import com.escapa.backend.application.model.CompanyEmployeeCounts;
import com.escapa.backend.application.port.CompanyEmployeeRepositoryPort;
import com.escapa.backend.domain.user.UserStatus;

/**
 * Vínculos colaborador-empresa em memória, só o que o cadastro, o reenvio de convite, a compra e a
 * edição usam.
 */
public final class FakeEmployeeLinks implements CompanyEmployeeRepositoryPort {

    private final Map<UUID, Set<UUID>> links = new LinkedHashMap<>();
    private final Map<String, String> jobTitles = new HashMap<>();
    private String lastJobTitle;

    @Override
    public void linkEmployee(UUID companyId, UUID userId, String jobTitle) {
        links.computeIfAbsent(companyId, ignored -> new LinkedHashSet<>()).add(userId);
        jobTitles.put(key(companyId, userId), jobTitle);
        lastJobTitle = jobTitle;
    }

    /** Só o cargo e o vínculo são guardados aqui; nome, e-mail e status vêm de quem chama. */
    @Override
    public Optional<CompanyEmployee> findEmployee(UUID companyId, UUID userId) {
        if (!isEmployeeOfCompany(companyId, userId)) {
            return Optional.empty();
        }
        return Optional.of(new CompanyEmployee(userId, null, null, jobTitles.get(key(companyId, userId)),
                UserStatus.ACTIVE, 2, "https://cdn/avatar.png"));
    }

    @Override
    public void updateJobTitle(UUID companyId, UUID userId, String jobTitle) {
        jobTitles.put(key(companyId, userId), jobTitle);
    }

    String jobTitle(UUID companyId, UUID userId) {
        return jobTitles.get(key(companyId, userId));
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

    private static String key(UUID companyId, UUID userId) {
        return companyId + ":" + userId;
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

package com.escapa.backend.application.usecase;

import com.escapa.backend.application.port.CompanySeatsPort;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Assentos de empresas em memória: o total comprado e quantos colaboradores os ocupam. */
public final class FakeCompanySeatsPort implements CompanySeatsPort {

    private final Map<UUID, Integer> seatsTotal = new HashMap<>();
    private final Map<UUID, Integer> seatsInUse = new HashMap<>();

    public void givenCompany(UUID companyId, int total, int inUse) {
        seatsTotal.put(companyId, total);
        seatsInUse.put(companyId, inUse);
    }

    @Override
    public int findSeatsTotal(UUID companyId) {
        return seatsTotal.get(companyId);
    }

    @Override
    public int countSeatsInUse(UUID companyId) {
        return seatsInUse.get(companyId);
    }

    @Override
    public void updateSeatsTotal(UUID companyId, int total) {
        seatsTotal.put(companyId, total);
    }
}

package com.escapa.backend.application.usecase;

import com.escapa.backend.application.dto.PageResult;
import com.escapa.backend.application.model.CompanyEmployee;
import com.escapa.backend.application.model.CompanyEmployeeCounts;
import com.escapa.backend.application.model.CompanySeatsSummary;
import com.escapa.backend.application.port.CompanyEmployeeRepositoryPort;
import com.escapa.backend.application.port.CompanySeatsPort;
import com.escapa.backend.domain.user.UserStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class GetCompanySeatsSummaryUseCaseTest {

    private final UUID companyA = UUID.randomUUID();
    private final UUID companyB = UUID.randomUUID();

    private FakeCompany fake;
    private GetCompanySeatsSummaryUseCase useCase;

    @BeforeEach
    void setUp() {
        fake = new FakeCompany();
        useCase = new GetCompanySeatsSummaryUseCase(fake, fake);
    }

    private void givenEmployees(UUID company, int active, int pending, int inactive) {
        for (int index = 0; index < active; index++) {
            fake.link(company, UserStatus.ACTIVE);
        }
        for (int index = 0; index < pending; index++) {
            fake.link(company, UserStatus.PENDING);
        }
        for (int index = 0; index < inactive; index++) {
            fake.link(company, UserStatus.INACTIVE);
        }
    }

    @Test
    void shouldSummarizeTheSeatsOfTheCompany() {
        fake.seatsTotal.put(companyA, 10);
        givenEmployees(companyA, 6, 2, 1);

        final CompanySeatsSummary summary = useCase.execute(companyA);

        assertEquals(new CompanySeatsSummary(10, 9, 1, 9, 6), summary);
    }

    @Test
    void shouldCountEmployeesOfAnyStatusAsSeatsInUse() {
        fake.seatsTotal.put(companyA, 10);
        givenEmployees(companyA, 0, 3, 0);

        final CompanySeatsSummary onlyPending = useCase.execute(companyA);

        assertEquals(3, onlyPending.seatsUsed());
        assertEquals(7, onlyPending.seatsAvailable());
        assertEquals(0, onlyPending.employeesActive());

        fake.employeesOf.get(companyA).clear();
        givenEmployees(companyA, 0, 0, 4);

        final CompanySeatsSummary onlyInactive = useCase.execute(companyA);

        assertEquals(4, onlyInactive.seatsUsed());
        assertEquals(6, onlyInactive.seatsAvailable());
        assertEquals(4, onlyInactive.employeesTotal());
        assertEquals(0, onlyInactive.employeesActive());
    }

    @Test
    void shouldReportAllSeatsFreeWhenThereAreNoEmployees() {
        fake.seatsTotal.put(companyA, 10);

        assertEquals(new CompanySeatsSummary(10, 0, 10, 0, 0), useCase.execute(companyA));
    }

    @Test
    void shouldReportNoSeatsForACompanyThatNeverBoughtAny() {
        fake.seatsTotal.put(companyA, 0);
        givenEmployees(companyA, 1, 0, 0);

        assertEquals(new CompanySeatsSummary(0, 1, 0, 1, 1), useCase.execute(companyA));
    }

    @Test
    void shouldNeverReportNegativeAvailableSeats() {
        fake.seatsTotal.put(companyA, 2);
        givenEmployees(companyA, 3, 1, 0);

        final CompanySeatsSummary summary = useCase.execute(companyA);

        assertEquals(0, summary.seatsAvailable());
        assertEquals(4, summary.seatsUsed());
    }

    @Test
    void shouldKeepCompaniesIsolatedFromEachOther() {
        fake.seatsTotal.put(companyA, 10);
        fake.seatsTotal.put(companyB, 3);
        givenEmployees(companyA, 6, 2, 1);
        givenEmployees(companyB, 1, 0, 0);

        assertEquals(new CompanySeatsSummary(10, 9, 1, 9, 6), useCase.execute(companyA));
        assertEquals(new CompanySeatsSummary(3, 1, 2, 1, 1), useCase.execute(companyB));
    }

    @Test
    void shouldRefuseAMissingCompanyId() {
        assertThrows(IllegalArgumentException.class, () -> useCase.execute(null));
    }

    /** Vagas e colaboradores em memória, por empresa. */
    private static final class FakeCompany implements CompanySeatsPort, CompanyEmployeeRepositoryPort {

        private final Map<UUID, Integer> seatsTotal = new HashMap<>();
        private final Map<UUID, List<UserStatus>> employeesOf = new HashMap<>();

        private void link(UUID company, UserStatus status) {
            employeesOf.computeIfAbsent(company, ignored -> new ArrayList<>()).add(status);
        }

        @Override
        public int findSeatsTotal(UUID companyId) {
            return seatsTotal.getOrDefault(companyId, 0);
        }

        @Override
        public int countSeatsInUse(UUID companyId) {
            return employeesOf.getOrDefault(companyId, List.of()).size();
        }

        @Override
        public void updateSeatsTotal(UUID companyId, int total) {
            seatsTotal.put(companyId, total);
        }

        @Override
        public CompanyEmployeeCounts countEmployees(UUID companyId) {
            final List<UserStatus> statuses = employeesOf.getOrDefault(companyId, List.of());
            return new CompanyEmployeeCounts(
                    statuses.size(), (int) statuses.stream().filter(UserStatus.ACTIVE::equals).count());
        }

        @Override
        public PageResult<CompanyEmployee> findEmployees(UUID companyId, String search, int page, int size) {
            throw new UnsupportedOperationException("not used by the seats summary");
        }
    }
}

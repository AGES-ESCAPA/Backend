package com.escapa.backend.application.usecase;

import com.escapa.backend.application.dto.PageResult;
import com.escapa.backend.application.model.CompanyEmployee;
import com.escapa.backend.application.model.CompanyEmployeeCounts;
import com.escapa.backend.application.port.CompanyEmployeeRepositoryPort;
import com.escapa.backend.domain.user.UserStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ListCompanyEmployeesUseCaseTest {

    private final UUID companyA = UUID.randomUUID();
    private final UUID companyB = UUID.randomUUID();

    private FakeEmployees fake;
    private ListCompanyEmployeesUseCase useCase;

    @BeforeEach
    void setUp() {
        fake = new FakeEmployees();
        useCase = new ListCompanyEmployeesUseCase(fake);
        fake.add(companyA, "Carla Souza", "carla@empresa-a.com.br");
        fake.add(companyA, "ana paula ferreira", "ana.ferreira@empresa-a.com.br");
        fake.add(companyA, "Bruno Alves", "bruno@empresa-a.com.br");
        fake.add(companyB, "Ana Beatriz Lima", "ana.lima@empresa-b.com.br");
    }

    private List<String> names(PageResult<CompanyEmployee> page) {
        return page.content().stream().map(CompanyEmployee::name).toList();
    }

    @Test
    void shouldListTheEmployeesOfTheCompanyOrderedByName() {
        final PageResult<CompanyEmployee> page = useCase.execute(companyA, null, 0, 10);

        assertEquals(List.of("ana paula ferreira", "Bruno Alves", "Carla Souza"), names(page));
    }

    @Test
    void shouldNeverListEmployeesOfAnotherCompany() {
        assertEquals(List.of("Ana Beatriz Lima"), names(useCase.execute(companyB, null, 0, 10)));
        assertTrue(names(useCase.execute(companyA, null, 0, 10)).stream().noneMatch("Ana Beatriz Lima"::equals));
    }

    @Test
    void shouldNotFindAnEmployeeOfAnotherCompanyEvenSearchingByTheirEmail() {
        assertTrue(useCase.execute(companyA, "empresa-b", 0, 10).content().isEmpty());
        assertTrue(useCase.execute(companyA, "Ana Beatriz", 0, 10).content().isEmpty());
    }

    @Test
    void shouldReturnAnEmptyPageForACompanyWithoutEmployees() {
        final PageResult<CompanyEmployee> page = useCase.execute(UUID.randomUUID(), null, 0, 10);

        assertTrue(page.content().isEmpty());
        assertEquals(0, page.totalElements());
        assertEquals(0, page.totalPages());
    }

    @Test
    void shouldSearchByNameIgnoringCase() {
        assertEquals(List.of("Bruno Alves"), names(useCase.execute(companyA, "BRUNO", 0, 10)));
        assertEquals(List.of("ana paula ferreira"), names(useCase.execute(companyA, "Paula", 0, 10)));
    }

    @Test
    void shouldSearchByEmailIgnoringCase() {
        assertEquals(List.of("Carla Souza"), names(useCase.execute(companyA, "CARLA@EMPRESA", 0, 10)));
        assertEquals(
                List.of("ana paula ferreira", "Bruno Alves", "Carla Souza"),
                names(useCase.execute(companyA, "empresa-a.com.br", 0, 10)));
    }

    @Test
    void shouldFindAPartOfTheNameInTheMiddle() {
        assertEquals(List.of("ana paula ferreira"), names(useCase.execute(companyA, "paula fer", 0, 10)));
    }

    @Test
    void shouldTrimTheSearchTermBeforeLookingForIt() {
        useCase.execute(companyA, "  bruno  ", 0, 10);

        assertEquals("bruno", fake.lastSearch);
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "   ", "\t"})
    void shouldNotFilterWhenTheSearchIsBlank(String blank) {
        final PageResult<CompanyEmployee> page = useCase.execute(companyA, blank, 0, 10);

        assertNull(fake.lastSearch);
        assertEquals(3, page.totalElements());
    }

    @Test
    void shouldReturnNothingWhenNoEmployeeMatchesTheSearch() {
        final PageResult<CompanyEmployee> page = useCase.execute(companyA, "zzz", 0, 10);

        assertTrue(page.content().isEmpty());
        assertEquals(0, page.totalElements());
    }

    @Test
    void shouldPaginateTheEmployees() {
        final PageResult<CompanyEmployee> first = useCase.execute(companyA, null, 0, 2);
        final PageResult<CompanyEmployee> second = useCase.execute(companyA, null, 1, 2);

        assertEquals(List.of("ana paula ferreira", "Bruno Alves"), names(first));
        assertEquals(List.of("Carla Souza"), names(second));
        assertEquals(3, first.totalElements());
        assertEquals(2, first.totalPages());
        assertEquals(0, first.pageNumber());
        assertEquals(1, second.pageNumber());
        assertEquals(2, second.pageSize());
    }

    @Test
    void shouldReturnAnEmptyPageBeyondTheLastOne() {
        final PageResult<CompanyEmployee> page = useCase.execute(companyA, null, 5, 2);

        assertTrue(page.content().isEmpty());
        assertEquals(3, page.totalElements());
    }

    @Test
    void shouldPaginateTheSearchResultsToo() {
        final PageResult<CompanyEmployee> page = useCase.execute(companyA, "empresa-a", 1, 2);

        assertEquals(List.of("Carla Souza"), names(page));
        assertEquals(3, page.totalElements());
    }

    @ParameterizedTest
    @ValueSource(ints = {-1, -10})
    void shouldRefuseANegativePage(int page) {
        assertThrows(IllegalArgumentException.class, () -> useCase.execute(companyA, null, page, 10));
    }

    @ParameterizedTest
    @ValueSource(ints = {0, -1, 101, 1000})
    void shouldRefuseAPageSizeOutsideTheLimits(int size) {
        assertThrows(IllegalArgumentException.class, () -> useCase.execute(companyA, null, 0, size));
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 50, 100})
    void shouldAcceptTheLimitsOfThePageSize(int size) {
        assertEquals(size, useCase.execute(companyA, null, 0, size).pageSize());
    }

    @Test
    void shouldRefuseAMissingCompanyId() {
        assertThrows(IllegalArgumentException.class, () -> useCase.execute(null, null, 0, 10));
    }

    /**
     * Colaboradores em memória que imitam o adaptador: só os da empresa pedida, busca por nome ou
     * e-mail sem diferenciar maiúsculas, ordem por nome e paginação.
     */
    private static final class FakeEmployees implements CompanyEmployeeRepositoryPort {

        private final Map<UUID, List<CompanyEmployee>> employeesOf = new HashMap<>();
        private String lastSearch;

        private void add(UUID company, String name, String email) {
            employeesOf.computeIfAbsent(company, ignored -> new ArrayList<>()).add(
                    new CompanyEmployee(UUID.randomUUID(), name, email, "Recepção", UserStatus.ACTIVE, 0, null));
        }

        @Override
        public CompanyEmployeeCounts countEmployees(UUID companyId) {
            throw new UnsupportedOperationException("not used by the employees list");
        }

        @Override
        public PageResult<CompanyEmployee> findEmployees(UUID companyId, String search, int page, int size) {
            lastSearch = search;
            final String term = search == null ? null : search.toLowerCase(Locale.ROOT);
            final List<CompanyEmployee> matching = employeesOf.getOrDefault(companyId, List.of()).stream()
                    .filter(employee -> term == null
                            || employee.name().toLowerCase(Locale.ROOT).contains(term)
                            || employee.email().toLowerCase(Locale.ROOT).contains(term))
                    .sorted(Comparator.comparing(employee -> employee.name().toLowerCase(Locale.ROOT)))
                    .toList();

            final int from = Math.min(page * size, matching.size());
            final int to = Math.min(from + size, matching.size());
            final int totalPages = (matching.size() + size - 1) / size;
            return new PageResult<>(matching.subList(from, to), page, size, matching.size(), totalPages);
        }
    }
}

package com.escapa.backend.application.usecase;

import com.escapa.backend.application.dto.PageResult;
import com.escapa.backend.application.model.CompanyEmployeeProgress;
import com.escapa.backend.application.model.CompanyReportFigures;
import com.escapa.backend.application.model.CompanyReportPeriod;
import com.escapa.backend.application.model.EmployeeProgressStatus;
import com.escapa.backend.application.port.CompanyReportRepositoryPort;
import com.escapa.backend.domain.company.CompanyNotFoundException;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ListCompanyEmployeeProgressUseCaseTest {

    private static final UUID COMPANY_ID = UUID.fromString("d0000000-0000-4000-d000-000000000001");

    private final FakePort port = new FakePort();
    private final ListCompanyEmployeeProgressUseCase useCase = new ListCompanyEmployeeProgressUseCase(port);

    @Test
    void passesTheCompanyStatusAndPaginationToThePort() {
        final PageResult<CompanyEmployeeProgress> result =
                useCase.execute(COMPANY_ID, "ana", EmployeeProgressStatus.COMPLETED, 2, 15);

        assertSame(port.result, result);
        assertEquals(COMPANY_ID, port.companyId);
        assertEquals("ana", port.search);
        assertEquals(EmployeeProgressStatus.COMPLETED, port.status);
        assertEquals(2, port.page);
        assertEquals(15, port.size);
    }

    @Test
    void searchIsStrippedIncludingUnicodeSpaces() {
        useCase.execute(COMPANY_ID, "\u3000 Ana Paula \u3000", null, 0, 10);

        assertEquals("Ana Paula", port.search);
    }

    @Test
    void blankSearchAndNullStatusDoNotFilter() {
        useCase.execute(COMPANY_ID, "   ", null, 0, 10);

        assertNull(port.search);
        assertNull(port.status);
    }

    @Test
    void searchWithNulCharacterIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> useCase.execute(COMPANY_ID, "an\u0000a", null, 0, 10));
    }

    @Test
    void negativePageIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> useCase.execute(COMPANY_ID, null, null, -1, 10));
    }

    @Test
    void unknownCompanyIsRejected() {
        port.companyExists = false;

        assertThrows(CompanyNotFoundException.class, () -> useCase.execute(COMPANY_ID, null, null, 0, 10));
    }

    @Test
    void nullCompanyIdIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> useCase.execute(null, null, null, 0, 10));
    }

    private static final class FakePort implements CompanyReportRepositoryPort {

        private final PageResult<CompanyEmployeeProgress> result = new PageResult<>(List.of(), 0, 10, 0, 0);
        private boolean companyExists = true;
        private UUID companyId;
        private String search;
        private EmployeeProgressStatus status;
        private int page;
        private int size;

        @Override
        public boolean existsCompany(UUID id) {
            return companyExists;
        }

        @Override
        public CompanyReportFigures loadFigures(UUID id, CompanyReportPeriod period) {
            throw new UnsupportedOperationException();
        }

        @Override
        public PageResult<CompanyEmployeeProgress> findEmployeeProgress(
                UUID id, String searchTerm, EmployeeProgressStatus statusFilter, int pageNumber, int pageSize) {
            this.companyId = id;
            this.search = searchTerm;
            this.status = statusFilter;
            this.page = pageNumber;
            this.size = pageSize;
            return result;
        }
    }
}

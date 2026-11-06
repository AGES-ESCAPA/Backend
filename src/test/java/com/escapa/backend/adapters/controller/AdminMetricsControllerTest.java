package com.escapa.backend.adapters.controller;

import com.escapa.backend.adapters.exception.GlobalExceptionHandler;
import com.escapa.backend.adapters.security.AdminRequestGuard;
import com.escapa.backend.adapters.security.AuthenticatedUser;
import com.escapa.backend.application.model.EnrollmentCompletionCounts;
import com.escapa.backend.application.port.AdminMetricsRepositoryPort;
import com.escapa.backend.application.usecase.AuthorizeAdminUseCase;
import com.escapa.backend.application.usecase.GetAdminMetricsOverviewUseCase;
import com.escapa.backend.application.usecase.InMemoryUserRepositoryPort;
import com.escapa.backend.domain.entity.User;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AdminMetricsControllerTest {
    private static final String URL = "/api/v1/admin/metrics/overview";
    private final InMemoryUserRepositoryPort users = new InMemoryUserRepositoryPort();
    private MockMvc mockMvc;
    private UUID adminId;
    private UUID studentId;

    @BeforeEach
    void setUp() {
        adminId = users.save(new User("Admin", "admin@escapa.com", "hash", "ADMIN")).getId();
        studentId = users.save(new User("Student", "student@escapa.com", "hash", "STUDENT")).getId();
        final AdminMetricsController controller = new AdminMetricsController(
                new GetAdminMetricsOverviewUseCase(new EmptyRepository(),
                        Clock.fixed(Instant.parse("2026-11-05T12:00:00Z"), ZoneOffset.UTC)),
                new AdminRequestGuard(new AuthorizeAdminUseCase(users)));
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler()).build();
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void clearAuthentication() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldRejectMissingAuthentication() throws Exception {
        mockMvc.perform(get(URL)).andExpect(status().isUnauthorized());
    }

    @Test
    void shouldRejectNonAdmin() throws Exception {
        authenticate(studentId, "STUDENT");
        mockMvc.perform(get(URL)).andExpect(status().isForbidden());
    }

    @Test
    void shouldReturnContractForAdmin() throws Exception {
        authenticate(adminId, "ADMIN");
        mockMvc.perform(get(URL)).andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Operation completed successfully"))
                .andExpect(jsonPath("$.data.activeStudents").value(0))
                .andExpect(jsonPath("$.data.partnerCompanies").value(0))
                .andExpect(jsonPath("$.data.averageCompletionRate").value(0.00))
                .andExpect(jsonPath("$.data.activeStudentsByMonth.length()").value(6))
                .andExpect(jsonPath("$.data.activeStudentsByMonth[0].month").value("2026-06"))
                .andExpect(jsonPath("$.data.activeStudentsByMonth[5].month").value("2026-11"));
    }

    private static void authenticate(UUID userId, String role) {
        final AuthenticatedUser user = new AuthenticatedUser(userId, "test@escapa.com", role, role);
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                user, null, AuthorityUtils.createAuthorityList("ROLE_" + role)));
    }

    private static final class EmptyRepository implements AdminMetricsRepositoryPort {
        @Override
        public long countActiveStudents(LocalDate date) {
            return 0;
        }

        @Override
        public long countPartnerCompanies() {
            return 0;
        }

        @Override
        public EnrollmentCompletionCounts countHistoricalCompletion() {
            return new EnrollmentCompletionCounts(0, 0);
        }

        @Override
        public long countActiveStudentsInPeriod(LocalDate from, LocalDate to) {
            return 0;
        }
    }
}

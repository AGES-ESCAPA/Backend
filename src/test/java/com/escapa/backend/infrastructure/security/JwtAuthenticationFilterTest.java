package com.escapa.backend.infrastructure.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

import com.escapa.backend.adapters.security.AuthenticatedUser;
import com.escapa.backend.application.model.TokenClaims;
import com.escapa.backend.application.port.TokenProviderPort;
import com.escapa.backend.application.usecase.AuthorizeActiveUserUseCase;
import com.escapa.backend.application.usecase.InMemoryUserRepositoryPort;
import com.escapa.backend.domain.entity.User;
import com.escapa.backend.domain.user.UserStatus;
import com.escapa.backend.domain.user.UserTypes;

class JwtAuthenticationFilterTest {

    private final InMemoryUserRepositoryPort users = new InMemoryUserRepositoryPort();
    private ControllableTokenProvider tokens;
    private JwtAuthenticationFilter filter;
    private boolean chainCalled;

    @BeforeEach
    void setUp() {
        tokens = new ControllableTokenProvider();
        filter = new JwtAuthenticationFilter(tokens, new AuthorizeActiveUserUseCase(users));
        chainCalled = false;
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void clearAuthentication() {
        SecurityContextHolder.clearContext();
    }

    private FilterChain chain() {
        return (request, response) -> chainCalled = true;
    }

    @Test
    void shouldContinueWithoutAuthWhenAuthorizationHeaderIsMissing() throws Exception {
        filter.doFilter(new MockHttpServletRequest(), new MockHttpServletResponse(), chain());

        assertTrue(chainCalled);
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void shouldIgnoreNonBearerOrEmptyBearerTokens() throws Exception {
        final MockHttpServletRequest basic = new MockHttpServletRequest();
        basic.addHeader(HttpHeaders.AUTHORIZATION, "Basic abc");
        filter.doFilter(basic, new MockHttpServletResponse(), chain());
        assertNull(SecurityContextHolder.getContext().getAuthentication());

        chainCalled = false;
        final MockHttpServletRequest emptyBearer = new MockHttpServletRequest();
        emptyBearer.addHeader(HttpHeaders.AUTHORIZATION, "Bearer ");
        filter.doFilter(emptyBearer, new MockHttpServletResponse(), chain());

        assertTrue(chainCalled);
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void shouldAuthenticateActiveUserFromValidBearerToken() throws Exception {
        final User user = users.save(new User("Mariana", "mariana@email.com", "hash", UserTypes.STUDENT));
        tokens.claims = new TokenClaims(user.getId(), user.getEmail(), UserTypes.STUDENT, UserTypes.STUDENT);

        final MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer valid-token");
        filter.doFilter(request, new MockHttpServletResponse(), chain());

        assertTrue(chainCalled);
        final AuthenticatedUser principal =
                (AuthenticatedUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        assertEquals(user.getId(), principal.id());
        assertEquals(UserTypes.STUDENT, principal.role());
    }

    @Test
    void shouldClearContextWhenTokenIsInvalidOrUserIsInactive() throws Exception {
        tokens.failWith = new IllegalArgumentException("bad token");
        final MockHttpServletRequest invalid = new MockHttpServletRequest();
        invalid.addHeader(HttpHeaders.AUTHORIZATION, "Bearer bad");
        filter.doFilter(invalid, new MockHttpServletResponse(), chain());
        assertNull(SecurityContextHolder.getContext().getAuthentication());

        final User inactive = users.save(new User("Carla", "carla@email.com", "hash", UserTypes.STUDENT));
        inactive.setStatus(UserStatus.INACTIVE);
        tokens.failWith = null;
        tokens.claims = new TokenClaims(inactive.getId(), inactive.getEmail(), UserTypes.STUDENT, UserTypes.STUDENT);
        final MockHttpServletRequest inactiveRequest = new MockHttpServletRequest();
        inactiveRequest.addHeader(HttpHeaders.AUTHORIZATION, "Bearer inactive");
        filter.doFilter(inactiveRequest, new MockHttpServletResponse(), chain());

        assertTrue(chainCalled);
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    private static final class ControllableTokenProvider implements TokenProviderPort {
        private TokenClaims claims;
        private RuntimeException failWith;

        @Override
        public String generateToken(UUID userId, String email, String role, String profile) {
            return "token";
        }

        @Override
        public long getExpirationSeconds() {
            return 3600L;
        }

        @Override
        public TokenClaims validateToken(String token) {
            if (failWith != null) {
                throw failWith;
            }
            return claims;
        }
    }
}

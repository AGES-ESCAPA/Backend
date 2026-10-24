package com.escapa.backend.infrastructure.security;

import java.io.IOException;

import com.escapa.backend.adapters.security.AuthenticatedUser;
import com.escapa.backend.application.model.TokenClaims;
import com.escapa.backend.application.port.TokenProviderPort;
import com.escapa.backend.application.usecase.AuthorizeActiveUserUseCase;
import com.escapa.backend.domain.user.InactiveUserException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";

    private final TokenProviderPort tokenProviderPort;
    private final AuthorizeActiveUserUseCase authorizeActiveUserUseCase;

    public JwtAuthenticationFilter(
            TokenProviderPort tokenProviderPort,
            AuthorizeActiveUserUseCase authorizeActiveUserUseCase
    ) {
        this.tokenProviderPort = tokenProviderPort;
        this.authorizeActiveUserUseCase = authorizeActiveUserUseCase;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        final String authorization = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (authorization == null || authorization.isBlank()) {
            filterChain.doFilter(request, response);
            return;
        }
        if (!authorization.startsWith(BEARER_PREFIX) || authorization.length() == BEARER_PREFIX.length()) {
            continueWithoutAuthentication(request, response, filterChain);
            return;
        }

        try {
            final TokenClaims claims = tokenProviderPort.validateToken(authorization.substring(BEARER_PREFIX.length()));
            // Conta desativada (US-24) invalida o token na hora, sem esperar ele expirar.
            authorizeActiveUserUseCase.execute(claims.userId());
            final AuthenticatedUser user = new AuthenticatedUser(
                    claims.userId(), claims.email(), claims.role(), claims.profile());
            final var authentication = new UsernamePasswordAuthenticationToken(
                    user, null, AuthorityUtils.createAuthorityList("ROLE_" + claims.role()));
            SecurityContextHolder.getContext().setAuthentication(authentication);
            filterChain.doFilter(request, response);
        } catch (IllegalArgumentException | InactiveUserException ex) {
            continueWithoutAuthentication(request, response, filterChain);
        }
    }

    private void continueWithoutAuthentication(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws IOException, ServletException {
        // SecurityConfig libera rotas publicas ou devolve 401 para as protegidas.
        SecurityContextHolder.clearContext();
        filterChain.doFilter(request, response);
    }
}

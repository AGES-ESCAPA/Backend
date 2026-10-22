package com.escapa.backend.infrastructure.security;

import java.io.IOException;

import com.escapa.backend.adapters.security.AuthenticatedUser;
import com.escapa.backend.application.model.TokenClaims;
import com.escapa.backend.application.port.TokenProviderPort;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";

    private final TokenProviderPort tokenProviderPort;
    private final SecurityErrorResponseWriter errorResponseWriter;

    public JwtAuthenticationFilter(TokenProviderPort tokenProviderPort, SecurityErrorResponseWriter errorResponseWriter) {
        this.tokenProviderPort = tokenProviderPort;
        this.errorResponseWriter = errorResponseWriter;
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
            errorResponseWriter.write(request, response, HttpStatus.UNAUTHORIZED, "Invalid access token");
            return;
        }

        try {
            final TokenClaims claims = tokenProviderPort.validateToken(authorization.substring(BEARER_PREFIX.length()));
            final AuthenticatedUser user = new AuthenticatedUser(
                    claims.userId(), claims.email(), claims.role(), claims.profile());
            final var authentication = new UsernamePasswordAuthenticationToken(
                    user, null, AuthorityUtils.createAuthorityList("ROLE_" + claims.role()));
            SecurityContextHolder.getContext().setAuthentication(authentication);
            filterChain.doFilter(request, response);
        } catch (IllegalArgumentException ex) {
            SecurityContextHolder.clearContext();
            errorResponseWriter.write(request, response, HttpStatus.UNAUTHORIZED, "Invalid or expired access token");
        }
    }
}

package com.escapa.backend.infrastructure.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;

    public JwtAuthenticationFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        final String token = recoverToken(request);
        String authUserId = null;
        String role = null;

        if (token != null) {
            authUserId = jwtService.validateTokenAndGetUserId(token);
            role = jwtService.validateTokenAndGetRole(token);
        } else {
            final String headerId = request.getHeader("X-User-Id");
            if (headerId != null && !headerId.trim().isEmpty()) {
                authUserId = headerId.trim();
            }
        }

        if (authUserId != null && !authUserId.isEmpty()) {
            final List<GrantedAuthority> authorities = role != null && !role.isEmpty() ? List.of(new SimpleGrantedAuthority("ROLE_" + role.toUpperCase())) : Collections.emptyList();
            final UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(authUserId, null, authorities);
            SecurityContextHolder.getContext().setAuthentication(authentication);
            
            final String finalUserId = authUserId;
            final HttpServletRequest wrappedRequest = new HttpServletRequestWrapper(request) {
                @Override
                public String getHeader(String name) {
                    if ("X-User-Id".equalsIgnoreCase(name)) { return finalUserId; }
                    return super.getHeader(name);
                }
                
                @Override
                public Enumeration<String> getHeaders(String name) {
                    if ("X-User-Id".equalsIgnoreCase(name)) { return Collections.enumeration(List.of(finalUserId)); }
                    return super.getHeaders(name);
                }
            };
            filterChain.doFilter(wrappedRequest, response);
            return;
        }

        filterChain.doFilter(request, response);
    }

    private String recoverToken(HttpServletRequest request) {
        final String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return null;
        }
        return authHeader.replace("Bearer ", "");
    }
}

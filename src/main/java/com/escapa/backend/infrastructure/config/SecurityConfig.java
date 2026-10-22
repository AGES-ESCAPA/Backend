package com.escapa.backend.infrastructure.config;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import com.escapa.backend.infrastructure.security.JwtAuthenticationFilter;
import com.escapa.backend.infrastructure.security.SecurityErrorResponseWriter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final SecurityErrorResponseWriter errorResponseWriter;

    public SecurityConfig(
            JwtAuthenticationFilter jwtAuthenticationFilter,
            SecurityErrorResponseWriter errorResponseWriter
    ) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.errorResponseWriter = errorResponseWriter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(csrf -> csrf.disable())
                .cors(Customizer.withDefaults())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(this::writeUnauthorized)
                        .accessDeniedHandler(this::writeForbidden))
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers("/api/v1/auth/me").authenticated()
                        .requestMatchers(
                                "/", "/error", "/actuator/health", "/api/v1/health", "/swagger-ui/**", "/swagger-ui.html",
                                "/v3/api-docs/**",
                                "/api/v1/auth/**", "/api/v1/public/**")
                        .permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/certificates/*").permitAll()
                        .requestMatchers("/api/v1/admin/**", "/api/v1/users/**").hasRole("ADMIN")
                        .anyRequest().authenticated())
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource(
            @Value("${app.cors.allowed-origins}") String allowedOrigins
    ) {
        final CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(Arrays.stream(allowedOrigins.split(","))
                .map(String::trim)
                .filter(origin -> !origin.isEmpty())
                .toList());
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("Authorization", "Content-Type", "Accept"));
        configuration.setExposedHeaders(List.of("Authorization"));

        final UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    private void writeUnauthorized(HttpServletRequest request, HttpServletResponse response, Exception exception)
            throws IOException {
        errorResponseWriter.write(request, response, HttpStatus.UNAUTHORIZED, "Missing or invalid access token");
    }

    private void writeForbidden(HttpServletRequest request, HttpServletResponse response, Exception exception)
            throws IOException {
        errorResponseWriter.write(request, response, HttpStatus.FORBIDDEN, "Access denied");
    }
}

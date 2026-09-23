package com.escapa.backend.infrastructure.security;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTCreationException;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.escapa.backend.application.port.TokenServicePort;
import com.escapa.backend.domain.entity.User;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

@Service
public class JwtService implements TokenServicePort {

    @Value("${api.security.token.secret:my-super-secret-key-for-dev}")
    private String secret;

    @Value("${api.security.token.expiration_hours:24}")
    private Integer expirationHours;

    @Override
    public String generateToken(User user) {
        try {
            final Algorithm algorithm = Algorithm.HMAC256(secret);
            return JWT.create()
                    .withIssuer("escapa-api")
                    .withSubject(user.getId().toString())
                    .withClaim("role", user.getUserType())
                    .withExpiresAt(genExpirationDate())
                    .sign(algorithm);
        } catch (JWTCreationException exception) {
            throw new RuntimeException("Error while generating token", exception);
        }
    }

    @Override
    public String validateTokenAndGetUserId(String token) {
        try {
            final Algorithm algorithm = Algorithm.HMAC256(secret);
            return JWT.require(algorithm)
                    .withIssuer("escapa-api")
                    .build()
                    .verify(token)
                    .getSubject();
        } catch (JWTVerificationException exception) {
            return "";
        }
    }

    @Override
    public String validateTokenAndGetRole(String token) {
        try {
            final Algorithm algorithm = Algorithm.HMAC256(secret);
            return JWT.require(algorithm)
                    .withIssuer("escapa-api")
                    .build()
                    .verify(token)
                    .getClaim("role").asString();
        } catch (JWTVerificationException exception) {
            return "";
        }
    }

    private Instant genExpirationDate() {
        return LocalDateTime.now().plusHours(expirationHours).toInstant(ZoneOffset.of("-03:00"));
    }
}

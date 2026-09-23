package com.escapa.backend.application.port;

import com.escapa.backend.domain.entity.User;

public interface TokenServicePort {
    String generateToken(User user);
    String validateTokenAndGetUserId(String token);
    String validateTokenAndGetRole(String token);
}

package com.escapa.backend.domain.entity;

import java.time.LocalDateTime;
import java.util.UUID;

import com.escapa.backend.domain.user.UserTypes;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = {"id", "email"})
public class User {
    private UUID id;
    private String name;
    private String email;
    private String passwordHash;
    private String userType;
    private String avatarUrl;
    private LocalDateTime createdAt;

    public User(String name, String email, String passwordHash, String userType) {
        this(null, name, email, passwordHash, userType, null, LocalDateTime.now());
    }

    public boolean isAdmin() {
        return UserTypes.ADMIN.equalsIgnoreCase(userType);
    }
}
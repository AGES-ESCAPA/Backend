package com.escapa.backend.domain.entity;

import java.time.LocalDateTime;
import java.util.UUID;

import com.escapa.backend.domain.user.UserStatus;
import com.escapa.backend.domain.user.UserTypes;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode(of = {"id", "email"})
public class User {
    private UUID id;
    private String name;
    private String email;
    private String passwordHash;
    private String userType;
    private String avatarUrl;
    private UserStatus status = UserStatus.ACTIVE;
    private LocalDateTime createdAt;

    public User(UUID id, String name, String email, String passwordHash, String userType,
                String avatarUrl, UserStatus status, LocalDateTime createdAt) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.passwordHash = passwordHash;
        this.userType = userType;
        this.avatarUrl = avatarUrl;
        this.status = status != null ? status : UserStatus.ACTIVE;
        this.createdAt = createdAt;
    }

    public User(UUID id, String name, String email, String passwordHash, String userType,
                String avatarUrl, LocalDateTime createdAt) {
        this(id, name, email, passwordHash, userType, avatarUrl, UserStatus.ACTIVE, createdAt);
    }

    public User(String name, String email, String passwordHash, String userType) {
        this(null, name, email, passwordHash, userType, null, UserStatus.ACTIVE, LocalDateTime.now());
    }

    public boolean isAdmin() {
        return UserTypes.ADMIN.equalsIgnoreCase(userType);
    }
}
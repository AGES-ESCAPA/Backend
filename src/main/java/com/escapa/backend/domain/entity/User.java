package com.escapa.backend.domain.entity;

import com.escapa.backend.domain.user.UserTypes;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

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
    private LocalDateTime createdAt;

    public User(String name, String email, String passwordHash, String userType) {
        this(null, name, email, passwordHash, userType, LocalDateTime.now());
    }

    public boolean isAdmin() {
        return UserTypes.ADMIN.equalsIgnoreCase(userType);
    }
}

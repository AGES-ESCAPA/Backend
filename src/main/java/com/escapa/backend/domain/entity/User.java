package com.escapa.backend.domain.entity;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

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
    private String status;
    private LocalDateTime createdAt;
    private List<UserCourse> userCourses = new ArrayList<>();

    public User(UUID id, String name, String email, String passwordHash, String userType, LocalDateTime createdAt) {
        this(id, name, email, passwordHash, userType, "ACTIVE", createdAt, new ArrayList<>());
    }

    public User(UUID id, String name, String email, String passwordHash, String userType,
                 LocalDateTime createdAt, List<UserCourse> userCourses) {
        this(id, name, email, passwordHash, userType, "ACTIVE", createdAt, userCourses);
    }

    public User(String name, String email, String passwordHash, String userType) {
        this(null, name, email, passwordHash, userType, "ACTIVE", LocalDateTime.now(), new ArrayList<>());
    }
}
package com.escapa.backend.domain.entity;

import com.escapa.backend.domain.user.UserStatus;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

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
    private UserStatus status = UserStatus.ACTIVE;
    private LocalDateTime createdAt;
    private List<UserCourse> userCourses = new ArrayList<>();

    public User(UUID id, String name, String email, String passwordHash, String userType,
                UserStatus status, LocalDateTime createdAt, List<UserCourse> userCourses) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.passwordHash = passwordHash;
        this.userType = userType;
        this.status = status != null ? status : UserStatus.ACTIVE;
        this.createdAt = createdAt;
        this.userCourses = userCourses != null ? userCourses : new ArrayList<>();
    }

    public User(UUID id, String name, String email, String passwordHash, String userType,
                LocalDateTime createdAt, List<UserCourse> userCourses) {
        this(id, name, email, passwordHash, userType, UserStatus.ACTIVE, createdAt, userCourses);
    }

    public User(UUID id, String name, String email, String passwordHash, String userType,
                UserStatus status, LocalDateTime createdAt) {
        this(id, name, email, passwordHash, userType, status, createdAt, new ArrayList<>());
    }

    public User(UUID id, String name, String email, String passwordHash, String userType, LocalDateTime createdAt) {
        this(id, name, email, passwordHash, userType, UserStatus.ACTIVE, createdAt, new ArrayList<>());
    }

    public User(String name, String email, String passwordHash, String userType) {
        this(null, name, email, passwordHash, userType, UserStatus.ACTIVE, LocalDateTime.now(), new ArrayList<>());
    }
}

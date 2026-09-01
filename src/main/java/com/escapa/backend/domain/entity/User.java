package com.escapa.backend.domain.entity;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = {"id", "email"})
public class User {
    private Integer id;
    private String email;
    private String password;
    private String userType;
    private LocalDateTime createdAt;
    private List<UserCourse> userCourses = new ArrayList<>();

    public User(Integer id, String email, String password, String userType, LocalDateTime createdAt) {
        this(id, email, password, userType, createdAt, new ArrayList<>());
    }

    public User(String email, String password, String userType) {
        this(null, email, password, userType, LocalDateTime.now(), new ArrayList<>());
    }
}
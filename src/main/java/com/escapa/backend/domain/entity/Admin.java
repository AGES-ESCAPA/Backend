package com.escapa.backend.domain.entity;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true, of = "department")
public class Admin extends User {
    private String department;
    private List<Course> courses = new ArrayList<>();

    public Admin(UUID id, String name, String email, String passwordHash, String userType, LocalDateTime createdAt,
                 String department) {
        super(id, name, email, passwordHash, userType, createdAt, new ArrayList<>());
        this.department = department;
    }
}

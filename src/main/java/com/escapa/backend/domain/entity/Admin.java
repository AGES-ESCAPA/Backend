package com.escapa.backend.domain.entity;

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
@EqualsAndHashCode(callSuper = true, of = "department")
public class Admin extends User {
    private String department;
    private List<Course> courses = new ArrayList<>();

    public Admin(Integer id, String email, String password, String userType, LocalDateTime createdAt, String department) {
        super(id, email, password, userType, createdAt, new ArrayList<>());
        this.department = department;
    }
}
package com.escapa.backend.domain.entity;

import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;

@NoArgsConstructor
public class Employee extends User {

    public Employee(Integer id, String email, String password, String userType, LocalDateTime createdAt) {
        super(id, email, password, userType, createdAt, new ArrayList<>());
    }
}
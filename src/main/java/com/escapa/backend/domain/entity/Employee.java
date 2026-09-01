package com.escapa.backend.domain.entity;

import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.ArrayList;

@NoArgsConstructor
public class Employee extends User {

    public Employee(UUID id, String name, String email, String passwordHash, String userType,
                    LocalDateTime createdAt) {
        super(id, name, email, passwordHash, userType, createdAt, new ArrayList<>());
    }
}

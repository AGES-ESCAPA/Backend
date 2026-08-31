package com.escapa.backend.infrastructure.persistence.entity;

import com.escapa.backend.infrastructure.persistence.UserEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrimaryKeyJoinColumn;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "admins")
@PrimaryKeyJoinColumn(name = "user_id")
@Getter
@Setter
@NoArgsConstructor
public class AdminEntity extends UserEntity {

    @Column(name = "department")
    private String department;

    @OneToMany(mappedBy = "createdBy")
    private List<CourseEntity> courses = new ArrayList<>();

    public AdminEntity(String id, String name, String email, String role, String department) {
        super(id, name, email, role);
        this.department = department;
    }
}

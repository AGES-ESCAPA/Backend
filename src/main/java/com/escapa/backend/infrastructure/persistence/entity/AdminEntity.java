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

import java.time.LocalDateTime;
import java.util.UUID;
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

    @Column(name = "headline")
    private String headline;

    @Column(name = "bio", columnDefinition = "TEXT")
    private String bio;

    @Column(name = "avatar_url")
    private String avatarUrl;

    @OneToMany(mappedBy = "createdBy")
    private List<CourseEntity> courses = new ArrayList<>();

    @OneToMany(mappedBy = "instructor")
    private List<CourseEntity> taughtCourses = new ArrayList<>();

    public AdminEntity(UUID id, String name, String email, String passwordHash, String role,
                       LocalDateTime createdAt, String department) {
        super(id, name, email, passwordHash, role, createdAt);
        this.department = department;
    }
}

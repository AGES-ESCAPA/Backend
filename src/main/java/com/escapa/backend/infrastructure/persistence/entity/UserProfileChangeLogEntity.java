package com.escapa.backend.infrastructure.persistence.entity;

import java.time.LocalDateTime;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "user_profile_change_log")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UserProfileChangeLogEntity {

    private static final int FIELD_NAME_MAX_LENGTH = 64;

    @Id
    @Column(nullable = false)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "changed_by")
    private UUID changedBy;

    @Column(name = "field_name", nullable = false, length = FIELD_NAME_MAX_LENGTH)
    private String fieldName;

    @Column(name = "old_value", columnDefinition = "TEXT")
    private String oldValue;

    @Column(name = "new_value", columnDefinition = "TEXT")
    private String newValue;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}

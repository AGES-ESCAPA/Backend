package com.escapa.backend.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_content_progress")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UserContentProgressEntity {

    @EmbeddedId
    private UserContentProgressId id;

    @Column(name = "completed_at", nullable = false)
    private LocalDateTime completedAt;
}

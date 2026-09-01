package com.escapa.backend.infrastructure.persistence.entity;

import com.escapa.backend.infrastructure.persistence.UserEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

/** Historico de versoes do curso: o que mudou, quando e quem mudou. */
@Entity
@Table(name = "course_change_log")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CourseChangeLogEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "course_id", nullable = false)
    private CourseEntity course;

    /** Nulo quando a alteracao foi feita por um processo automatico ou por usuario ja removido. */
    @ManyToOne
    @JoinColumn(name = "changed_by")
    private UserEntity changedBy;

    @Column(name = "description", columnDefinition = "TEXT", nullable = false)
    private String description;

    @Column(name = "major_version", nullable = false)
    private Integer majorVersion = 0;

    @Column(name = "minor_version", nullable = false)
    private Integer minorVersion = 0;

    @Column(name = "created_at")
    private LocalDateTime createdAt;
}

package com.escapa.backend.infrastructure.persistence.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(
        name = "modules",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_modules_course_order",
                columnNames = {"course_id", "order"}
        )
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ModuleEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "course_id", nullable = false)
    private CourseEntity course;

    @Column(nullable = false)
    private String title;

    @Column(name = "\"order\"", nullable = false)
    private Integer order;

    @OneToMany(mappedBy = "module", cascade = CascadeType.ALL)
    @OrderBy("order ASC")
    private List<ContentEntity> contents = new ArrayList<>();

    /** Modulos que precisam ser concluidos antes deste ser liberado. */
    @OneToMany(mappedBy = "module", cascade = CascadeType.ALL)
    private List<ModulePrerequisiteEntity> prerequisites = new ArrayList<>();
}

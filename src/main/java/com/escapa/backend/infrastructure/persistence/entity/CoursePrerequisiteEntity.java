package com.escapa.backend.infrastructure.persistence.entity;

import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Relacao N:N autorreferenciada: cursos que precisam ser concluidos antes deste.
 * O banco impede apenas a autorreferencia direta; a deteccao de ciclos maiores
 * fica a cargo do caso de uso que grava o pre-requisito.
 */
@Entity
@Table(name = "course_prerequisites")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CoursePrerequisiteEntity {

    @EmbeddedId
    private CoursePrerequisiteId id;

    @ManyToOne
    @MapsId("courseId")
    @JoinColumn(name = "course_id", nullable = false)
    private CourseEntity course;

    @ManyToOne
    @MapsId("prerequisiteCourseId")
    @JoinColumn(name = "prerequisite_course_id", nullable = false)
    private CourseEntity prerequisiteCourse;
}

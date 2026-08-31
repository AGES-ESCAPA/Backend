package com.escapa.backend.infrastructure.persistence.entity;

import jakarta.persistence.Column;
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

import java.time.LocalDate;

@Entity
@Table(name = "company_courses")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CompanyCourseEntity {

    @EmbeddedId
    private CompanyCourseId id;

    @ManyToOne
    @MapsId("companyId")
    @JoinColumn(name = "company_id")
    private CompanyEntity company;

    @ManyToOne
    @MapsId("courseId")
    @JoinColumn(name = "course_id")
    private CourseEntity course;

    @Column(name = "data_inicio")
    private LocalDate dataInicio;

    @Column(name = "data_expiracao")
    private LocalDate dataExpiracao;
}

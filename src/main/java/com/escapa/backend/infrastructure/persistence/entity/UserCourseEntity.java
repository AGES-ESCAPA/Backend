package com.escapa.backend.infrastructure.persistence.entity;

import com.escapa.backend.infrastructure.persistence.UserEntity;
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
import java.time.LocalDateTime;

@Entity
@Table(name = "user_courses")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UserCourseEntity {

    @EmbeddedId
    private UserCourseId id;

    @ManyToOne
    @MapsId("userId")
    @JoinColumn(name = "user_id")
    private UserEntity user;

    @ManyToOne
    @MapsId("courseId")
    @JoinColumn(name = "course_id")
    private CourseEntity course;

    @Column(name = "dt_inicio")
    private LocalDate dtInicio;

    @Column(name = "dt_expiracao")
    private LocalDate dtExpiracao;

    @Column(name = "progress")
    private Integer progress;

    @Column(name = "conclusion_date")
    private LocalDate conclusionDate;

    @Column(name = "last_access_date")
    private LocalDateTime lastAccessDate;

    @Column(name = "certificate_issued", nullable = false)
    private Boolean certificateIssued = false;

    /** Codigo publico usado para validar a autenticidade do certificado emitido. */
    @Column(name = "certificate_code", unique = true)
    private String certificateCode;

    @Column(name = "certificate_issued_at")
    private LocalDateTime certificateIssuedAt;
}

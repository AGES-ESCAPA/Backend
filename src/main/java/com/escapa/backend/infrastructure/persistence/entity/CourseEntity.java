package com.escapa.backend.infrastructure.persistence.entity;

import com.escapa.backend.infrastructure.persistence.UserEntity;
import com.escapa.backend.infrastructure.persistence.entity.enums.CourseStatus;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "courses")
@Getter
@Setter
@NoArgsConstructor
public class CourseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "short_description", columnDefinition = "TEXT")
    private String shortDescription;

    @Column(name = "thumbnail_url")
    private String thumbnailUrl;

    @Column(name = "teaser_video_url")
    private String teaserVideoUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private CourseStatus status = CourseStatus.DRAFT;

    @ManyToOne
    @JoinColumn(name = "created_by")
    private UserEntity createdBy;

    @ManyToOne
    @JoinColumn(name = "instructor_id")
    private AdminEntity instructor;

    @Column(name = "category")
    private String category;

    @Column(name = "level")
    private String level;

    @Column(name = "duration_time")
    private Integer durationTime;

    @Column(name = "deadline")
    private Integer deadline;

    /** Dias de acesso concedidos ao aluno a partir da compra; alimenta user_courses.dt_expiracao. */
    @Column(name = "access_duration_days")
    private Integer accessDurationDays;

    @Column(name = "price")
    private Double price;

    @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.JSON)
    @Column(name = "learning_objectives", columnDefinition = "JSON")
    private String learningObjectives;

    /** Impede o aluno de pular aulas/modulos fora de ordem. */
    @Column(name = "require_sequential_progress", nullable = false)
    private Boolean requireSequentialProgress = true;

    /** Bloqueia o acesso ao conteudo depois que o prazo do curso expira. */
    @Column(name = "enforce_deadline_block", nullable = false)
    private Boolean enforceDeadlineBlock = false;

    @Column(name = "major_version", nullable = false)
    private Integer majorVersion = 0;

    @Column(name = "minor_version", nullable = false)
    private Integer minorVersion = 0;

    /** Contadores desnormalizados: mantidos na escrita para evitar agregacoes em cada listagem. */
    @Column(name = "lessons_count", nullable = false)
    private Integer lessonsCount = 0;

    @Column(name = "materials_count", nullable = false)
    private Integer materialsCount = 0;

    @Column(name = "students_count", nullable = false)
    private Integer studentsCount = 0;

    @Column(name = "reviews_count", nullable = false)
    private Integer reviewsCount = 0;

    @Column(name = "rating_average")
    private Double ratingAverage;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "course", cascade = CascadeType.ALL)
    @OrderBy("order ASC")
    private List<ModuleEntity> modules = new ArrayList<>();

    @OneToMany(mappedBy = "course")
    private List<UserCourseEntity> userCourses = new ArrayList<>();

    @OneToMany(mappedBy = "course")
    private List<CompanyCourseEntity> companyCourses = new ArrayList<>();

    @OneToMany(mappedBy = "course", cascade = CascadeType.ALL)
    @OrderBy("order ASC")
    private List<CourseMaterialEntity> materials = new ArrayList<>();

    @OneToMany(mappedBy = "course")
    private List<CourseReviewEntity> reviews = new ArrayList<>();

    @OneToMany(mappedBy = "course", cascade = CascadeType.ALL)
    private List<CoursePrerequisiteEntity> prerequisites = new ArrayList<>();

    @OneToMany(mappedBy = "course", cascade = CascadeType.ALL)
    @OrderBy("createdAt DESC")
    private List<CourseChangeLogEntity> changeLogs = new ArrayList<>();
}

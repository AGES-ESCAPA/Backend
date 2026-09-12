package com.escapa.backend.domain.entity;

import com.escapa.backend.domain.course.CourseStatus;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = {"id", "title"})
public class Course {
    private UUID id;
    private String title;
    private String description;
    private String shortDescription;
    private String thumbnailUrl;
    private String teaserVideoUrl;
    private CourseStatus status = CourseStatus.DRAFT;
    private User createdBy;
    private User instructor;
    private String category;
    private String level;
    private Integer durationTime;
    private Integer deadline;
    private Integer accessDurationDays;
    private Double price;
    private List<String> learningObjectives = new ArrayList<>();
    private Boolean requireSequentialProgress = true;
    private Boolean enforceDeadlineBlock = false;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private List<Module> modules = new ArrayList<>();
    private List<UserCourse> userCourses = new ArrayList<>();
    private List<CompanyCourse> companyCourses = new ArrayList<>();

    public Course(String title, String shortDescription, String description, String thumbnailUrl,
                  String teaserVideoUrl, User instructor, String category, String level,
                  Integer durationTime, Integer deadline, Integer accessDurationDays, Double price,
                  List<String> learningObjectives, Boolean requireSequentialProgress,
                  Boolean enforceDeadlineBlock, User createdBy) {
        this.title = title;
        this.shortDescription = shortDescription;
        this.description = description;
        this.thumbnailUrl = thumbnailUrl;
        this.teaserVideoUrl = teaserVideoUrl;
        this.instructor = instructor;
        this.category = category;
        this.level = level;
        this.durationTime = durationTime;
        this.deadline = deadline;
        this.accessDurationDays = accessDurationDays;
        this.price = price;
        this.learningObjectives = learningObjectives != null ? learningObjectives : new ArrayList<>();
        this.requireSequentialProgress = requireSequentialProgress != null ? requireSequentialProgress : true;
        this.enforceDeadlineBlock = enforceDeadlineBlock != null ? enforceDeadlineBlock : false;
        this.createdBy = createdBy;
        this.status = CourseStatus.DRAFT;
        this.createdAt = LocalDateTime.now();
    }
}
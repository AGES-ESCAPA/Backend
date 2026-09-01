package com.escapa.backend.domain.entity;

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
    private User createdBy;
    private String category;
    private String level;
    private Integer durationTime;
    private Integer deadline;
    private Double price;
    private LocalDateTime createdAt;
    private List<Module> modules = new ArrayList<>();
    private List<UserCourse> userCourses = new ArrayList<>();
    private List<CompanyCourse> companyCourses = new ArrayList<>();
}
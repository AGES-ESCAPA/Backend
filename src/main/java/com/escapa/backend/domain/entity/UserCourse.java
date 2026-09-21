package com.escapa.backend.domain.entity;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = {"user", "course"})
public class UserCourse {
    private User user;
    private Course course;
    private LocalDate dtInicio;
    private LocalDate dtExpiracao;
    private Integer progress;
    private LocalDate conclusionDate;
}
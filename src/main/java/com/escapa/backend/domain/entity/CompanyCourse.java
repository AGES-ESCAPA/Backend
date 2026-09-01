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
@EqualsAndHashCode(of = {"company", "course"})
public class CompanyCourse {
    private Company company;
    private Course course;
    private LocalDate dataInicio;
    private LocalDate dataExpiracao;
}
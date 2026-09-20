package com.escapa.backend.application.model;

import java.time.LocalDate;

/** Periodo de acesso de um aluno a um curso (user_courses). Datas nulas nao restringem o acesso. */
public record Enrollment(LocalDate startDate, LocalDate expirationDate) {
}

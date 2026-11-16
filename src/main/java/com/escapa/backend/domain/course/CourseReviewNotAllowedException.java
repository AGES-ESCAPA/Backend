package com.escapa.backend.domain.course;

/** Apenas o aluno que concluiu o curso pode avalia-lo. */
public class CourseReviewNotAllowedException extends RuntimeException {

    public CourseReviewNotAllowedException() {
        super("Only students who completed the course can rate it");
    }
}

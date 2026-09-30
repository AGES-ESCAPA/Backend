package com.escapa.backend.application.model;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Dados da tela de certificado digital (US-18): o certificado, o aluno que o
 * recebeu e o resumo do curso exibido no CourseCard ao lado.
 */
public record CertificateDetails(
        LocalDate conclusionDate,
        /** Carga horaria do curso, em minutos (Course.durationTime). */
        Integer workloadMinutes,
        String verificationCode,
        Student student,
        Course course
) {

    /**
     * {@code avatarUrl} so existe no perfil de admin (tabela admins), entao vem
     * nulo para alunos comuns. {@code verified} indica conta ativa.
     */
    public record Student(String name, String avatarUrl, boolean verified) {
    }

    /** {@code rating} e nulo enquanto o curso nao tiver avaliacoes. */
    public record Course(
            UUID id,
            String title,
            String description,
            String category,
            String level,
            String thumbnailUrl,
            Integer durationTime,
            Integer lessonsCount,
            Double rating,
            long reviewsCount,
            String instructor,
            Double price
    ) {
    }
}

package com.escapa.backend.application.model;

import java.time.LocalDate;
import java.util.Objects;
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
     * Regra unica de dono: decide o {@code isOwner} da tela (US-18) e libera o
     * download do PDF (US-19). {@code userId} nulo e visitante anonimo.
     */
    public boolean isOwnedBy(UUID userId) {
        return userId != null && Objects.equals(student.id(), userId);
    }

    /**
     * {@code avatarUrl} so existe no perfil de admin (tabela admins), entao vem
     * nulo para alunos comuns. {@code verified} indica conta ativa.
     */
    public record Student(UUID id, String name, String avatarUrl, boolean verified) {
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

package com.escapa.backend.application.port;

import java.time.LocalDate;
import java.util.UUID;

public interface EnrollmentProgressRepositoryPort {

    /**
     * Grava o percentual de conclusao da matricula do aluno no curso, exibido em
     * "Meus Cursos". Com {@code conclusionDate} preenchida, registra a conclusao do
     * curso se ainda nao houver uma. Sem matricula ativa, nao faz nada.
     */
    void updateProgress(UUID userId, UUID courseId, int progress, LocalDate conclusionDate);
}

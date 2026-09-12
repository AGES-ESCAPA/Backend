package com.escapa.backend.application.port;

import com.escapa.backend.application.dto.ChangeLogEntry;
import com.escapa.backend.application.dto.PageResult;

import java.util.List;
import java.util.UUID;

/**
 * Histórico de alterações de um curso (US-09). Trabalha só com tipos de
 * aplicação/domínio: a resolução para {@code CourseEntity}/{@code UserEntity}
 * (JPA) fica inteiramente na implementação de infraestrutura.
 */
public interface CourseChangeLogRepositoryPort {

    /** As {@code limit} entradas mais recentes, mais recente primeiro. */
    List<ChangeLogEntry> findRecentByCourseId(UUID courseId, int limit);

    /** Histórico paginado, mais recente primeiro. */
    PageResult<ChangeLogEntry> findPageByCourseId(UUID courseId, int page, int size);

    /**
     * Registra uma entrada de histórico.
     *
     * @param changedById id do admin autor da alteração, ou {@code null} para
     *                    alterações automáticas/sem autor ("Sistema")
     */
    void save(UUID courseId, UUID changedById, String description, int majorVersion, int minorVersion);
}

package com.escapa.backend.application.port;

import com.escapa.backend.domain.entity.Module;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ModuleRepositoryPort {
    boolean existsById(UUID id);

    Module save(Module module);

    Optional<Module> findById(UUID id);

    /** Modulos do curso ordenados por {@code order}, cada um com seus conteudos carregados. */
    List<Module> findByCourseId(UUID courseId);

    /** Proximo valor livre de ordem no curso: max(order) + 1, ou 1 quando vazio. */
    int nextOrder(UUID courseId);

    void deleteById(UUID id);

    /**
     * Regrava a ordem dos modulos do curso em duas fases, para nao violar
     * uk_modules_course_order enquanto as posicoes se cruzam.
     *
     * @param orderedIds ids na ordem final desejada; posicoes atribuidas a partir de 1
     */
    void reorder(UUID courseId, List<UUID> orderedIds);
}

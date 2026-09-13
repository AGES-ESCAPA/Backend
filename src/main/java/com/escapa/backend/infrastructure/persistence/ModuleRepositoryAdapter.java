package com.escapa.backend.infrastructure.persistence;

import com.escapa.backend.application.port.ModuleRepositoryPort;
import com.escapa.backend.domain.entity.Module;
import com.escapa.backend.domain.module.ModuleNotFoundException;
import com.escapa.backend.infrastructure.persistence.entity.CourseEntity;
import com.escapa.backend.infrastructure.persistence.entity.ModuleEntity;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class ModuleRepositoryAdapter implements ModuleRepositoryPort {
    private final ModuleJpaRepository moduleJpaRepository;
    private final CourseJpaRepository courseJpaRepository;

    public ModuleRepositoryAdapter(
            ModuleJpaRepository moduleJpaRepository,
            CourseJpaRepository courseJpaRepository
    ) {
        this.moduleJpaRepository = moduleJpaRepository;
        this.courseJpaRepository = courseJpaRepository;
    }

    @Override
    public boolean existsById(UUID id) {
        return id != null && moduleJpaRepository.existsById(id);
    }

    @Override
    @Transactional
    public Module save(Module module) {
        final ModuleEntity entity;
        if (module.getId() == null) {
            entity = new ModuleEntity();
        } else {
            entity = moduleJpaRepository.findById(module.getId())
                    .orElseThrow(() -> new ModuleNotFoundException(module.getId()));
        }
        final CourseEntity course = courseJpaRepository.getReferenceById(module.getCourseId());
        ModuleMapper.applyToEntity(module, entity, course);
        return ModuleMapper.toDomain(moduleJpaRepository.save(entity));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Module> findById(UUID id) {
        return moduleJpaRepository.findById(id).map(ModuleMapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Module> findByCourseId(UUID courseId) {
        return moduleJpaRepository.findByCourseIdOrderByOrderAsc(courseId).stream()
                .map(ModuleMapper::toDomain)
                .toList();
    }

    @Override
    public int nextOrder(UUID courseId) {
        final Integer maxOrder = moduleJpaRepository.findMaxOrderByCourseId(courseId);
        return maxOrder != null ? maxOrder + 1 : 1;
    }

    @Override
    @Transactional
    public void deleteById(UUID id) {
        // deleteById carrega a entidade e o cascade = ALL de ModuleEntity remove
        // conteudos e pre-requisitos; o ON DELETE CASCADE do banco cobre o resto.
        moduleJpaRepository.deleteById(id);
    }

    @Override
    @Transactional
    public void reorder(UUID courseId, List<UUID> orderedIds) {
        // Fase 1: estaciona todo mundo na faixa negativa. Sem isso, gravar a ordem
        // final direto violaria uk_modules_course_order no meio do caminho, porque
        // duas linhas dividiriam a mesma posicao ate a ultima atualizacao.
        for (int index = 0; index < orderedIds.size(); index++) {
            moduleJpaRepository.updateOrderByIdAndCourseId(orderedIds.get(index), courseId, -(index + 1));
        }
        // Fase 2: faixa positiva ja livre, grava a ordem definitiva.
        for (int index = 0; index < orderedIds.size(); index++) {
            moduleJpaRepository.updateOrderByIdAndCourseId(orderedIds.get(index), courseId, index + 1);
        }
    }
}

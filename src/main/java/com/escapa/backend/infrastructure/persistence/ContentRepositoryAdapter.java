package com.escapa.backend.infrastructure.persistence;

import com.escapa.backend.application.port.ContentRepositoryPort;
import com.escapa.backend.domain.entity.Content;
import com.escapa.backend.infrastructure.persistence.entity.ContentEntity;
import com.escapa.backend.infrastructure.persistence.entity.ModuleEntity;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class ContentRepositoryAdapter implements ContentRepositoryPort {

    private final ContentJpaRepository contentJpaRepository;
    private final ModuleJpaRepository moduleJpaRepository;

    public ContentRepositoryAdapter(
            ContentJpaRepository contentJpaRepository,
            ModuleJpaRepository moduleJpaRepository
    ) {
        this.contentJpaRepository = contentJpaRepository;
        this.moduleJpaRepository = moduleJpaRepository;
    }

    @Override
    public Content save(Content content) {
        final ContentEntity entity = content.getId() != null
                ? contentJpaRepository.findById(content.getId()).orElseGet(ContentEntity::new)
                : new ContentEntity();
        final ModuleEntity module = moduleJpaRepository.getReferenceById(content.getModuleId());
        ContentMapper.applyToEntity(content, entity, module);
        return ContentMapper.toDomain(contentJpaRepository.save(entity));
    }

    @Override
    public Optional<Content> findById(UUID id) {
        return contentJpaRepository.findById(id).map(ContentMapper::toDomain);
    }

    @Override
    public List<Content> findByModuleId(UUID moduleId) {
        return contentJpaRepository.findByModuleIdOrderByOrderAsc(moduleId).stream()
                .map(ContentMapper::toDomain)
                .toList();
    }

    @Override
    public int nextOrder(UUID moduleId) {
        final Integer maxOrder = contentJpaRepository.findMaxOrderByModuleId(moduleId);
        return maxOrder != null ? maxOrder + 1 : 1;
    }

    @Override
    public void deleteById(UUID id) {
        contentJpaRepository.deleteById(id);
    }

    @Override
    @Transactional
    public void reorder(UUID moduleId, List<UUID> orderedIds) {
        // Fase 1: estaciona todo mundo na faixa negativa. Sem isso, gravar a ordem
        // final direto violaria uk_content_module_order no meio do caminho, porque
        // duas linhas dividiriam a mesma posicao ate a ultima atualizacao.
        for (int index = 0; index < orderedIds.size(); index++) {
            contentJpaRepository.updateOrderById(orderedIds.get(index), -(index + 1));
        }
        // Fase 2: faixa positiva ja livre, grava a ordem definitiva.
        for (int index = 0; index < orderedIds.size(); index++) {
            contentJpaRepository.updateOrderById(orderedIds.get(index), index + 1);
        }
    }
}

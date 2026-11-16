package com.escapa.backend.application.usecase;

import com.escapa.backend.application.course.PublishedCurriculum;
import com.escapa.backend.application.port.ContentRepositoryPort;
import com.escapa.backend.domain.content.ContentNotFoundException;
import com.escapa.backend.domain.entity.Content;

import java.util.UUID;

/**
 * Em curso publicado, a aula some do construtor e continua visível para o aluno
 * até a publicação. Fora isso, a remoção é definitiva.
 */
public class DeleteContentUseCase {

    private final ContentRepositoryPort contentRepositoryPort;

    public DeleteContentUseCase(ContentRepositoryPort contentRepositoryPort) {
        this.contentRepositoryPort = contentRepositoryPort;
    }

    public void execute(UUID id) {
        final Content content = contentRepositoryPort.findById(id)
                .orElseThrow(() -> new ContentNotFoundException(id));
        if (!content.isHeldForPublish()) {
            contentRepositoryPort.deleteById(id);
            return;
        }
        content.setPendingDeletion(true);
        content.setOrder(PublishedCurriculum.parkedOrder(content.getOrder()));
        contentRepositoryPort.save(content);
    }
}

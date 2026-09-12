package com.escapa.backend.application.usecase;

import com.escapa.backend.application.port.ContentRepositoryPort;
import com.escapa.backend.domain.content.ContentNotFoundException;
import com.escapa.backend.domain.content.ContentType;
import com.escapa.backend.domain.content.ContentTypeRules;
import com.escapa.backend.domain.entity.Content;

import java.util.UUID;

public class UpdateContentUseCase {

    private final ContentRepositoryPort contentRepositoryPort;

    public UpdateContentUseCase(ContentRepositoryPort contentRepositoryPort) {
        this.contentRepositoryPort = contentRepositoryPort;
    }

    public Content execute(
            UUID id,
            String title,
            ContentType type,
            String url,
            Integer durationMinutes,
            String description,
            Boolean isFree
    ) {
        if (id == null) {
            throw new IllegalArgumentException("id is required");
        }
        if (title == null || title.trim().isBlank()) {
            throw new IllegalArgumentException("title is required");
        }

        final Content content = contentRepositoryPort.findById(id)
                .orElseThrow(() -> new ContentNotFoundException(id));

        // Trocar o tipo e limpar campos do tipo anterior e permitido; a validacao
        // condicional abaixo garante que a nova combinacao continue coerente.
        content.setTitle(title.trim());
        content.setType(type);
        content.setUrl(url);
        content.setDurationMinutes(durationMinutes);
        content.setDescription(description);
        content.setIsFree(isFree != null && isFree);

        ContentTypeRules.validate(content);
        return contentRepositoryPort.save(content);
    }
}

package com.escapa.backend.domain.entity;

import com.escapa.backend.domain.content.ContentPendingDraft;
import com.escapa.backend.domain.content.ContentResource;
import com.escapa.backend.domain.content.ContentType;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = {"id", "title"})
public class Content {
    private UUID id;
    private UUID moduleId;
    private String title;
    private String description;
    private ContentType type;
    private String url;
    private Integer durationMinutes;
    /** Aula liberada como amostra, sem necessidade de compra. */
    private Boolean isFree;
    private Integer order;
    /** Materiais complementares (links e arquivos de apoio). */
    private List<ContentResource> resources = new ArrayList<>();
    private LocalDateTime createdAt;
    /** Posição que o aluno vê. Null enquanto a aula ainda não foi publicada. */
    private Integer publishedOrder;
    /** Edição ainda não publicada de uma aula que já está no ar. */
    private ContentPendingDraft pendingDraft;
    private boolean pendingDeletion;
    /** Preenchido na leitura. Não é coluna: vem do status do curso. */
    private boolean coursePublished;

    public Content copy() {
        final Content copy = new Content();
        copy.id = id;
        copy.moduleId = moduleId;
        copy.title = title;
        copy.description = description;
        copy.type = type;
        copy.url = url;
        copy.durationMinutes = durationMinutes;
        copy.isFree = isFree;
        copy.order = order;
        copy.resources = resources == null ? new ArrayList<>() : new ArrayList<>(resources);
        copy.createdAt = createdAt;
        copy.publishedOrder = publishedOrder;
        copy.pendingDraft = pendingDraft;
        copy.pendingDeletion = pendingDeletion;
        copy.coursePublished = coursePublished;
        return copy;
    }

    /** Aula que o aluno já vê: a alteração fica no rascunho até publicar. */
    public boolean isHeldForPublish() {
        return coursePublished && publishedOrder != null;
    }
}

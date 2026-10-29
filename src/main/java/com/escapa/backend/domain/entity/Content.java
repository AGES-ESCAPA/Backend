package com.escapa.backend.domain.entity;

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
}

package com.escapa.backend.infrastructure.persistence.entity;

import java.io.Serializable;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class ContentConceptId implements Serializable {

    @Column(name = "content_id", nullable = false)
    private UUID contentId;

    @Column(name = "\"order\"", nullable = false)
    private Integer order;
}

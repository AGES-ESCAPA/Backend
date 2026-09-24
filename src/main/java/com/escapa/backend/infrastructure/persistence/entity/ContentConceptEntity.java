package com.escapa.backend.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "content_concepts")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ContentConceptEntity {

    @EmbeddedId
    private ContentConceptId id;

    @Column(name = "name", nullable = false, length = 100)
    private String name;
}

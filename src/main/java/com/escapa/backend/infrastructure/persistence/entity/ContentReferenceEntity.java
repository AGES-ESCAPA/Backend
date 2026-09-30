package com.escapa.backend.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "content_references")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ContentReferenceEntity {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "content_id", nullable = false)
    private UUID contentId;

    public static final int MAX_TITLE_LENGTH = 255;
    public static final int MAX_URL_LENGTH = 2048;

    @Column(name = "title", nullable = false, length = MAX_TITLE_LENGTH)
    private String title;

    @Column(name = "url", nullable = false, length = MAX_URL_LENGTH)
    private String url;

    @Column(name = "\"order\"", nullable = false)
    private Integer order;
}

package com.escapa.backend.domain.entity;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = {"id", "title"})
public class Module {
    private UUID id;
    /** Referencia leve ao curso; preenchida nos fluxos de CRUD de modulos (US-06). */
    private UUID courseId;
    /** Agregado completo do curso; preenchido quando o modulo vem de dentro de um Course. */
    private Course course;
    private String title;
    private Integer order;
    /** Posição que o aluno vê. Null enquanto o módulo ainda não foi publicado. */
    private Integer publishedOrder;
    /** Título ainda não publicado de um módulo que já está no ar. */
    private String pendingTitle;
    private boolean pendingDeletion;
    /** Preenchido na leitura. Não é coluna: vem do status do curso. */
    private boolean coursePublished;
    private List<Content> contents = new ArrayList<>();

    public Module(UUID id, Course course, String title, Integer order) {
        this.id = id;
        this.course = course;
        this.courseId = course != null ? course.getId() : null;
        this.title = title;
        this.order = order;
        this.contents = new ArrayList<>();
    }

    public Module(UUID id, UUID courseId, String title, Integer order) {
        this.id = id;
        this.courseId = courseId;
        this.title = title;
        this.order = order;
        this.contents = new ArrayList<>();
    }

    /** Quantidade de conteudos do modulo, derivada da lista carregada. */
    public int getTotalContents() {
        return contents == null ? 0 : contents.size();
    }

    /** Módulo que o aluno já vê: título, ordem e exclusão ficam no rascunho até publicar. */
    public boolean isHeldForPublish() {
        return coursePublished && publishedOrder != null;
    }

    /** Soma de durationMinutes dos conteudos; conteudos sem duracao contam como zero. */
    public int getTotalDurationMinutes() {
        if (contents == null) {
            return 0;
        }
        return contents.stream()
                .map(Content::getDurationMinutes)
                .filter(Objects::nonNull)
                .mapToInt(Integer::intValue)
                .sum();
    }
}

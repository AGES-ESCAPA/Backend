package com.escapa.backend.domain.entity;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = {"id", "title"})
public class Module {
    private Integer id;
    private Course course;
    private String title;
    private Integer order;
    private List<Content> contents = new ArrayList<>();

    public Module(Integer id, Course course, String title, Integer order) {
        this.id = id;
        this.course = course;
        this.title = title;
        this.order = order;
        this.contents = new ArrayList<>();
    }
}
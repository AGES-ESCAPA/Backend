package com.escapa.backend.course.management.repository;

import com.escapa.backend.course.shared.entity.CourseEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

/**
 * Escrita e leitura de entidade de curso, para o CRUD do admin.
 * A vitrine pública não usa este repositório; ela tem o seu, só de leitura,
 * em {@code course.catalog.repository.CourseCatalogRepository}.
 */
public interface CourseRepository extends JpaRepository<CourseEntity, UUID> {
}

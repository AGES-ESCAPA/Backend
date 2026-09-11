package com.escapa.backend.course.catalog.controller;

import com.escapa.backend.common.api.PageResponse;
import com.escapa.backend.course.catalog.dto.CourseCardResponse;
import com.escapa.backend.course.catalog.service.CourseCatalogService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Vitrine pública (US-01). Sem autenticação. Devolve a página direto, sem
 * {@code ApiResponse}, por contrato com o frontend definido na US.
 */
@RestController
@RequestMapping("/api/v1/public/courses")
public class CourseCatalogController {

    private final CourseCatalogService catalogService;

    public CourseCatalogController(CourseCatalogService catalogService) {
        this.catalogService = catalogService;
    }

    @GetMapping
    public ResponseEntity<PageResponse<CourseCardResponse>> list(
            @RequestParam(required = false) String title,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String level,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size
    ) {
        return ResponseEntity.ok(PageResponse.from(catalogService.listPublished(title, category, level, page, size)));
    }
}

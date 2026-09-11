package com.escapa.backend.course.catalog.service;

import com.escapa.backend.course.catalog.dto.CourseCardResponse;
import com.escapa.backend.course.catalog.repository.CourseCatalogRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

/**
 * Vitrine pública de cursos (US-01). Não tem regra que falhe: filtro sem resultado
 * devolve página vazia. O trabalho aqui é normalizar a entrada antes da consulta.
 */
@Service
public class CourseCatalogService {

    static final int DEFAULT_PAGE = 0;
    static final int DEFAULT_SIZE = 10;
    static final int MAX_SIZE = 100;

    private static final String NO_FILTER = "";
    private static final String MATCH_ALL = "%";

    private final CourseCatalogRepository catalogRepository;

    public CourseCatalogService(CourseCatalogRepository catalogRepository) {
        this.catalogRepository = catalogRepository;
    }

    @Transactional(readOnly = true)
    public Page<CourseCardResponse> listPublished(
            String title, String category, String level, Integer page, Integer size) {
        final int safePage = (page != null && page >= 0) ? page : DEFAULT_PAGE;
        final int safeSize = (size != null && size > 0) ? Math.min(size, MAX_SIZE) : DEFAULT_SIZE;
        return catalogRepository.findPublished(
                toLikePattern(normalize(title)),
                toExactMatch(normalize(category)),
                toExactMatch(normalize(level)),
                PageRequest.of(safePage, safeSize));
    }

    /** Espaços nas pontas fora; vazio vira ausente, para {@code ?category=} não mudar o resultado. */
    static String normalize(String value) {
        if (value == null) {
            return null;
        }
        final String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    /** Busca parcial, sem caixa, e com {@code %}, {@code _} e {@code !} do usuário tratados como texto. */
    static String toLikePattern(String value) {
        if (value == null) {
            return MATCH_ALL;
        }
        return MATCH_ALL + escapeLike(value.toLowerCase(Locale.ROOT)) + MATCH_ALL;
    }

    /** Igualdade sem caixa: {@code Iniciante} e {@code INICIANTE} são o mesmo nível. */
    static String toExactMatch(String value) {
        return value == null ? NO_FILTER : value.toLowerCase(Locale.ROOT);
    }

    private static String escapeLike(String value) {
        final String escape = String.valueOf(CourseCatalogRepository.LIKE_ESCAPE);
        return value
                .replace(escape, escape + escape)
                .replace("%", escape + "%")
                .replace("_", escape + "_");
    }
}

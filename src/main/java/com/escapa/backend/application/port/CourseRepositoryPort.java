package com.escapa.backend.application.port;

import com.escapa.backend.application.dto.CourseSummary;
import com.escapa.backend.application.dto.PageResult;
import com.escapa.backend.application.model.CourseDetails;

import java.util.Optional;
import java.util.UUID;

/**
 * Contrato de acesso ao repositório de cursos.
 * A implementação concreta vive na camada de infraestrutura.
 */
public interface CourseRepositoryPort {

    /**
     * Busca cursos publicados com filtros opcionais e paginação.
     *
     * @param title    filtro parcial e case-insensitive sobre o título (pode ser null)
     * @param category filtro exato por categoria (pode ser null)
     * @param level    filtro exato por nível (pode ser null)
     * @param page     número da página (0-based)
     * @param size     tamanho da página
     * @return página de resumos de cursos publicados
     */
    PageResult<CourseSummary> findPublished(String title, String category, String level, int page, int size);

    /**
     * Busca os detalhes completos de um curso publicado, incluindo instrutor,
     * materiais e a estrutura de módulos/aulas.
     *
     * @param id identificador do curso
     * @return detalhes do curso, vazio se não existir ou não estiver publicado
     */
    Optional<CourseDetails> findDetailsById(UUID id);
}

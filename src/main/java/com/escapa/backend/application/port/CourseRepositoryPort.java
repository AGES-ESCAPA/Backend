package com.escapa.backend.application.port;

import com.escapa.backend.application.dto.CourseSummary;
import com.escapa.backend.application.dto.PageResult;

/**
 * Contrato de acesso ao repositório de cursos.
 * A implementação concreta vive na camada de infraestrutura.
 */
public interface CourseRepositoryPort {

    /**
     * Busca cursos publicados com filtros opcionais e paginação.
     *
     * <p>Contrato dos filtros:
     * <ul>
     *   <li>{@code title}: busca parcial e case-insensitive; caracteres como
     *       {@code %} e {@code _} são tratados como texto literal, nunca como curinga.</li>
     *   <li>{@code category} e {@code level}: igualdade case-insensitive.</li>
     *   <li>Filtros {@code null} são ignorados.</li>
     * </ul>
     *
     * <p>A ordem dos resultados é estável entre páginas: cursos mais recentes
     * primeiro, com desempate pelo id.
     *
     * @param title    filtro parcial e case-insensitive sobre o título (pode ser null)
     * @param category filtro case-insensitive por categoria (pode ser null)
     * @param level    filtro case-insensitive por nível (pode ser null)
     * @param page     número da página (0-based)
     * @param size     tamanho da página
     * @return página de resumos de cursos publicados
     */
    PageResult<CourseSummary> findPublished(String title, String category, String level, int page, int size);
}

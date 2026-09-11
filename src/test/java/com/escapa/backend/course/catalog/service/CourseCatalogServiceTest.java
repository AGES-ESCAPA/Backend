package com.escapa.backend.course.catalog.service;

import com.escapa.backend.course.catalog.dto.CourseCardResponse;
import com.escapa.backend.course.catalog.repository.CourseCatalogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * O service só normaliza entrada. Então o teste confere exatamente o que chega ao repositório.
 */
@ExtendWith(MockitoExtension.class)
class CourseCatalogServiceTest {

    @Mock
    private CourseCatalogRepository repository;

    private CourseCatalogService service;

    private final ArgumentCaptor<String> title = ArgumentCaptor.forClass(String.class);
    private final ArgumentCaptor<String> category = ArgumentCaptor.forClass(String.class);
    private final ArgumentCaptor<String> level = ArgumentCaptor.forClass(String.class);
    private final ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);

    @BeforeEach
    void setUp() {
        service = new CourseCatalogService(repository);
        lenient().when(repository.findPublished(anyString(), anyString(), anyString(), any(Pageable.class)))
                .thenReturn(Page.empty());
    }

    @Test
    void shouldUseDefaultsWhenPaginationIsAbsent() {
        service.listPublished(null, null, null, null, null);

        captured();
        assertEquals(0, pageable.getValue().getPageNumber());
        assertEquals(10, pageable.getValue().getPageSize());
    }

    @Test
    void shouldClampNegativePageAndInvalidSize() {
        service.listPublished(null, null, null, -1, 0);

        captured();
        assertEquals(0, pageable.getValue().getPageNumber());
        assertEquals(10, pageable.getValue().getPageSize());
    }

    @Test
    void shouldCapSizeAtMaximum() {
        service.listPublished(null, null, null, 3, 500);

        captured();
        assertEquals(3, pageable.getValue().getPageNumber());
        assertEquals(100, pageable.getValue().getPageSize());
    }

    @Test
    void shouldSendMatchAllPatternsWhenFiltersAreNullOrBlank() {
        service.listPublished("   ", "", null, null, null);

        captured();
        assertEquals("%", title.getValue());
        assertEquals("", category.getValue());
        assertEquals("", level.getValue());
    }

    @Test
    void shouldTrimAndLowercaseFilters() {
        service.listPublished("  Atendimento ", " Hospitalidade ", " Iniciante ", null, null);

        captured();
        assertEquals("%atendimento%", title.getValue());
        assertEquals("hospitalidade", category.getValue());
        assertEquals("iniciante", level.getValue());
    }

    @Test
    void shouldEscapeLikeWildcardsInTitle() {
        service.listPublished("100%_ok!", null, null, null, null);

        captured();
        assertEquals("%100!%!_ok!!%", title.getValue());
    }

    @Test
    void shouldReturnRepositoryPageUntouched() {
        final Page<CourseCardResponse> page = new PageImpl<>(List.of());
        when(repository.findPublished(anyString(), anyString(), anyString(), any(Pageable.class)))
                .thenReturn(page);

        assertSame(page, service.listPublished(null, null, null, null, null));
    }

    private void captured() {
        verify(repository).findPublished(title.capture(), category.capture(), level.capture(), pageable.capture());
    }
}

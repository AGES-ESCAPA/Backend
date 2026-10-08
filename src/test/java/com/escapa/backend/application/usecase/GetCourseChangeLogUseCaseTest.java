package com.escapa.backend.application.usecase;

import com.escapa.backend.application.dto.ChangeLogEntry;
import com.escapa.backend.application.dto.PageResult;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class GetCourseChangeLogUseCaseTest {

    @Test
    void shouldReturnPagedChangeLogMostRecentFirst() {
        final InMemoryCourseChangeLogRepositoryPort changeLogRepository = new InMemoryCourseChangeLogRepositoryPort();
        final UUID courseId = UUID.randomUUID();
        changeLogRepository.save(courseId, null, "Primeira alteração.", 1, 0);
        changeLogRepository.save(courseId, null, "Segunda alteração.", 1, 1);
        final GetCourseChangeLogUseCase useCase = new GetCourseChangeLogUseCase(changeLogRepository);

        final PageResult<ChangeLogEntry> result = useCase.execute(courseId, 0, 20);

        assertEquals(2, result.totalElements());
        assertEquals("Segunda alteração.", result.content().get(0).description());
    }

    @Test
    void shouldRejectNegativePage() {
        final GetCourseChangeLogUseCase useCase = new GetCourseChangeLogUseCase(new InMemoryCourseChangeLogRepositoryPort());

        assertThrows(IllegalArgumentException.class, () -> useCase.execute(UUID.randomUUID(), -1, 20));
    }

    @Test
    void shouldRejectSizeOutsideTheAllowedRange() {
        final GetCourseChangeLogUseCase useCase = new GetCourseChangeLogUseCase(new InMemoryCourseChangeLogRepositoryPort());
        final UUID courseId = UUID.randomUUID();

        assertThrows(IllegalArgumentException.class, () -> useCase.execute(courseId, 0, 0));
        assertThrows(
                IllegalArgumentException.class,
                () -> useCase.execute(courseId, 0, GetCourseChangeLogUseCase.MAX_PAGE_SIZE + 1)
        );
    }
}

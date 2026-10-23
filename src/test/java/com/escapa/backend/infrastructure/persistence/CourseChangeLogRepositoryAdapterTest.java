package com.escapa.backend.infrastructure.persistence;

import com.escapa.backend.application.dto.ChangeLogEntry;
import com.escapa.backend.application.dto.PageResult;
import com.escapa.backend.application.port.CourseChangeLogRepositoryPort;
import com.escapa.backend.application.port.UserRepositoryPort;
import com.escapa.backend.domain.entity.User;
import com.escapa.backend.infrastructure.persistence.entity.CourseEntity;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Transactional
class CourseChangeLogRepositoryAdapterTest extends PostgresIntegrationTest {

    @Autowired
    private CourseChangeLogRepositoryPort changeLogRepositoryPort;

    @Autowired
    private CourseJpaRepository courseJpaRepository;

    @Autowired
    private UserRepositoryPort userRepositoryPort;

    @Test
    void shouldMapEntriesWithAndWithoutAuthorAndReturnPagedHistory() {
        final CourseEntity course = new CourseEntity();
        course.setTitle("Course history");
        courseJpaRepository.save(course);
        final User author = userRepositoryPort.save(
                new User("Admin Author", "history-" + course.getId() + "@example.com", "hash", "ADMIN"));

        changeLogRepositoryPort.save(course.getId(), null, "Automatic change", 1, 0);
        changeLogRepositoryPort.save(course.getId(), author.getId(), "Manual change", 1, 1);

        final PageResult<ChangeLogEntry> page = changeLogRepositoryPort.findPageByCourseId(course.getId(), 0, 1);
        final List<ChangeLogEntry> recent = changeLogRepositoryPort.findRecentByCourseId(course.getId(), 2);
        final Map<String, ChangeLogEntry> entries = recent.stream()
                .collect(Collectors.toMap(ChangeLogEntry::description, entry -> entry));

        assertEquals(2, page.totalElements());
        assertEquals(1, page.content().size());
        assertEquals(2, recent.size());
        assertEquals("Admin Author", entries.get("Manual change").changedByName());
        assertEquals("Sistema", entries.get("Automatic change").changedByName());
    }
}

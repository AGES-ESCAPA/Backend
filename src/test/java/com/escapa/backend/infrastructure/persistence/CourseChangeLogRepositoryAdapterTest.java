package com.escapa.backend.infrastructure.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.transaction.annotation.Transactional;

import com.escapa.backend.application.dto.ChangeLogEntry;
import com.escapa.backend.application.dto.PageResult;
import com.escapa.backend.application.port.CourseChangeLogRepositoryPort;
import com.escapa.backend.application.port.UserRepositoryPort;
import com.escapa.backend.domain.entity.User;
import com.escapa.backend.infrastructure.persistence.entity.CourseEntity;

@Transactional
class CourseChangeLogRepositoryAdapterTest extends PostgresIntegrationTest {

    @Autowired
    private CourseChangeLogRepositoryPort changeLogRepository;

    @Autowired
    private CourseJpaRepository courseJpaRepository;

    @Autowired
    @Qualifier("userRepositoryAdapter")
    private UserRepositoryPort userRepository;

    @Test
    void shouldReadRecentAndPagedHistoryWithAndWithoutAnAdmin() {
        final CourseEntity course = new CourseEntity();
        course.setTitle("Curso com histórico");
        final CourseEntity savedCourse = courseJpaRepository.save(course);
        final User admin = userRepository.save(
                new User("Admin Histórico", "history-" + course.getTitle() + "@example.com", "hash", "ADMIN"));
        changeLogRepository.save(savedCourse.getId(), null, "Publicação automática", 1, 0);
        changeLogRepository.save(savedCourse.getId(), admin.getId(), "Alteração manual", 1, 1);

        final List<ChangeLogEntry> recent = changeLogRepository.findRecentByCourseId(savedCourse.getId(), 10);
        final PageResult<ChangeLogEntry> page = changeLogRepository.findPageByCourseId(savedCourse.getId(), 0, 1);

        assertEquals(2, recent.size());
        assertEquals("Admin Histórico", recent.get(0).changedByName());
        assertEquals("Sistema", recent.get(1).changedByName());
        assertEquals(2, page.totalElements());
        assertEquals(1, page.content().size());
        assertEquals("Admin Histórico", page.content().get(0).changedByName());
    }
}

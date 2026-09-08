package com.escapa.backend.infrastructure.persistence;

import com.escapa.backend.application.port.CourseRepositoryPort;
import com.escapa.backend.domain.entity.Course;
import com.escapa.backend.infrastructure.persistence.entity.AdminEntity;
import com.escapa.backend.infrastructure.persistence.entity.CourseEntity;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Repository
public class CourseRepositoryAdapter implements CourseRepositoryPort {

    private final CourseJpaRepository courseJpaRepository;
    private final UserJpaRepository userJpaRepository;
    private final EntityManager entityManager;

    public CourseRepositoryAdapter(CourseJpaRepository courseJpaRepository, UserJpaRepository userJpaRepository, EntityManager entityManager) {
        this.courseJpaRepository = courseJpaRepository;
        this.userJpaRepository = userJpaRepository;
        this.entityManager = entityManager;
    }

    @Override
    public Course save(Course course) {
        final CourseEntity entity = CourseMapper.toEntity(course);
        
        if (course.getInstructor() != null) {
            final AdminEntity admin = entityManager.find(AdminEntity.class, course.getInstructor().getId());
            entity.setInstructor(admin);
        }
        
        if (course.getCreatedBy() != null) {
             entity.setCreatedBy(userJpaRepository.findById(course.getCreatedBy().getId()).orElse(null));
        }

        final CourseEntity saved = courseJpaRepository.save(entity);
        return CourseMapper.toDomain(saved);
    }

    @Override
    public Optional<Course> findById(UUID id) {
        return courseJpaRepository.findById(id).map(CourseMapper::toDomain);
    }

    @Override
    public List<Course> findAll() {
        return courseJpaRepository.findAll().stream()
                .map(CourseMapper::toDomain)
                .collect(Collectors.toList());
    }
}

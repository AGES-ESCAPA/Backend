package com.escapa.backend.application.port;

import com.escapa.backend.application.model.CourseDetails;

import java.util.Optional;
import java.util.UUID;

public interface CourseRepositoryPort {

    Optional<CourseDetails> findDetailsById(UUID id);
}
package com.escapa.backend.adapters.controller;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.escapa.backend.adapters.dto.ApiResponse;
import com.escapa.backend.adapters.dto.StudentCourseCurriculumResponse;
import com.escapa.backend.adapters.dto.StudentLessonResponse;
import com.escapa.backend.application.model.LessonDetails;
import com.escapa.backend.application.model.StudentCourseCurriculum;
import com.escapa.backend.application.usecase.GetStudentCourseCurriculumUseCase;
import com.escapa.backend.application.usecase.GetStudentLessonUseCase;

/**
 * Endpoints do aluno (US-11). O aluno e identificado pelo header provisorio
 * {@code X-User-Id}, o mesmo das rotas /admin, ate o login da US-23 trazer o token.
 */
@RestController
@RequestMapping("/api/v1/student")
public class StudentLessonController {

    private final GetStudentLessonUseCase getStudentLessonUseCase;
    private final GetStudentCourseCurriculumUseCase getStudentCourseCurriculumUseCase;
    
    public StudentLessonController(
        GetStudentLessonUseCase getStudentLessonUseCase,
        GetStudentCourseCurriculumUseCase getStudentCourseCurriculumUseCase
    ) {
    this.getStudentLessonUseCase = getStudentLessonUseCase;
    this.getStudentCourseCurriculumUseCase = getStudentCourseCurriculumUseCase;
}

    @GetMapping("/courses/{courseId}/lessons/{lessonId}")
    public ResponseEntity<ApiResponse<StudentLessonResponse>> getLesson(
            @RequestHeader(value = "X-User-Id", required = false) String xUserId,
            @PathVariable UUID courseId,
            @PathVariable UUID lessonId
    ) {
        final UUID userId = requireUserId(xUserId);
        final LessonDetails lesson = getStudentLessonUseCase.execute(userId, courseId, lessonId);
        return ResponseEntity.ok(ApiResponse.success(StudentLessonResponse.from(lesson)));
    }

    @GetMapping("/courses/{courseId}/curriculum")
    public ResponseEntity<ApiResponse<StudentCourseCurriculumResponse>> getCourseCurriculum(
        @RequestHeader(value = "X-User-Id", required = false) String xUserId,
        @PathVariable UUID courseId
    ) {
    final UUID userId = requireUserId(xUserId);
    final StudentCourseCurriculum curriculum = getStudentCourseCurriculumUseCase.execute(userId, courseId);
    return ResponseEntity.ok(ApiResponse.success(StudentCourseCurriculumResponse.from(curriculum)));
}

    private UUID requireUserId(String xUserId) {
        if (xUserId == null || xUserId.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing X-User-Id header");
        }
        try {
            return UUID.fromString(xUserId.trim());
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid X-User-Id: must be a valid UUID");
        }
    }
}

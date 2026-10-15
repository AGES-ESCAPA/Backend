package com.escapa.backend.adapters.controller;

import com.escapa.backend.adapters.dto.ApiResponse;
import com.escapa.backend.adapters.dto.StudentCourseCurriculumResponse;
import com.escapa.backend.adapters.dto.StudentLessonResponse;
import com.escapa.backend.adapters.security.UserIdHeader;
import com.escapa.backend.application.model.LessonDetails;
import com.escapa.backend.application.model.StudentCourseCurriculum;
import com.escapa.backend.application.usecase.GetStudentCourseCurriculumUseCase;
import com.escapa.backend.application.usecase.GetStudentLessonUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * Endpoints do aluno (US-11). O aluno e identificado pelo header provisorio
 * {@code X-User-Id}, o mesmo das rotas /admin, ate o login da US-23 trazer o token.
 */
@RestController
@Tag(name = "Student - Lessons", description = "Lessons and curriculum of an enrolled student")
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
    @Operation(summary = "Get a lesson the student can access")
    public ResponseEntity<ApiResponse<StudentLessonResponse>> getLesson(
            @RequestHeader(value = UserIdHeader.NAME, required = false) String xUserId,
            @PathVariable UUID courseId,
            @PathVariable UUID lessonId
    ) {
        final UUID userId = UserIdHeader.requireStudentId(xUserId);
        final LessonDetails lesson = getStudentLessonUseCase.execute(userId, courseId, lessonId);
        return ResponseEntity.ok(ApiResponse.success(StudentLessonResponse.from(lesson)));
    }

    @GetMapping("/courses/{courseId}/curriculum")
    @Operation(summary = "Get the lesson grid of an enrolled course")
    public ResponseEntity<ApiResponse<StudentCourseCurriculumResponse>> getCourseCurriculum(
            @RequestHeader(value = UserIdHeader.NAME, required = false) String xUserId,
            @PathVariable UUID courseId
    ) {
        final UUID userId = UserIdHeader.requireStudentId(xUserId);
        final StudentCourseCurriculum curriculum = getStudentCourseCurriculumUseCase.execute(userId, courseId);
        return ResponseEntity.ok(ApiResponse.success(StudentCourseCurriculumResponse.from(curriculum)));
    }
}

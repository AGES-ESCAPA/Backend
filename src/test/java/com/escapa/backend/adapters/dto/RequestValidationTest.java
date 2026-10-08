package com.escapa.backend.adapters.dto;

import com.escapa.backend.adapters.dto.course.CreateCourseRequest;
import com.escapa.backend.adapters.dto.course.UpdateCourseRequest;
import com.escapa.backend.domain.content.ContentType;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RequestValidationTest {

    private static final String TOO_LONG = "x".repeat(FieldLimits.VARCHAR_MAX + 1);

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void shouldAcceptAMinimalCourseCreation() {
        final CreateCourseRequest request = createCourse("Curso", 0, 10.0);

        assertTrue(validator.validate(request).isEmpty());
    }

    @Test
    void shouldRejectNegativeNumbersAndOversizedTitleOnCourseCreation() {
        final CreateCourseRequest request = createCourse(TOO_LONG, -1, -5.0);

        assertEquals(
                Set.of("title must have at most 255 characters", "durationTime must not be negative",
                        "price must not be negative"),
                messages(validator.validate(request))
        );
    }

    @Test
    void shouldRejectNegativeNumbersOnCourseUpdate() {
        final UpdateCourseRequest request = new UpdateCourseRequest(
                null, null, null, null, null, null, null, null, -1, -2, -3, -4.0, null, null, null);

        assertEquals(
                Set.of("durationTime must not be negative", "deadline must not be negative",
                        "accessDurationDays must not be negative", "price must not be negative"),
                messages(validator.validate(request))
        );
    }

    @Test
    void shouldRejectBlankTitleOnModuleAndContentRequests() {
        assertEquals(Set.of("title is required"), messages(validator.validate(new ModuleRequest(" "))));
        assertEquals(
                Set.of("title is required", "type is required"),
                messages(validator.validate(new ContentRequest("", null, null, null, null, null)))
        );
    }

    @Test
    void shouldRejectOversizedContentUrl() {
        final ContentRequest request = new ContentRequest("Aula", ContentType.VIDEO, TOO_LONG, 5, null, false);

        assertEquals(Set.of("url must have at most 255 characters"), messages(validator.validate(request)));
    }

    @Test
    void shouldRejectNullIdsInReorderRequests() {
        final List<UUID> idsWithNull = java.util.Arrays.asList(UUID.randomUUID(), null);

        assertEquals(
                Set.of("moduleIds must not contain null"),
                messages(validator.validate(new ReorderModulesRequest(idsWithNull)))
        );
        assertEquals(
                Set.of("contentIds must not contain null"),
                messages(validator.validate(new ReorderContentsRequest(idsWithNull)))
        );
    }

    private static CreateCourseRequest createCourse(String title, Integer durationTime, Double price) {
        return new CreateCourseRequest(
                title, null, null, null, null, null, null, null, durationTime, null, null, price, null, null, null);
    }

    private static Set<String> messages(Set<? extends ConstraintViolation<?>> violations) {
        return violations.stream().map(ConstraintViolation::getMessage).collect(Collectors.toSet());
    }
}

package com.escapa.backend.application.usecase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.escapa.backend.application.model.Enrollment;
import com.escapa.backend.domain.company.CompanyEmployeeNotFoundException;
import com.escapa.backend.domain.course.CourseNotFoundException;

class AssignEmployeeCourseUseCaseTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 11, 21, 12, 0);
    private static final Clock CLOCK = Clock.fixed(NOW.atZone(ZoneId.systemDefault()).toInstant(), ZoneId.systemDefault());
    private static final LocalDate TODAY = NOW.toLocalDate();
    private static final UUID COMPANY_ID = UUID.randomUUID();
    private static final UUID EMPLOYEE_ID = UUID.randomUUID();
    private static final UUID COURSE_ID = UUID.randomUUID();

    private final FakeEmployeeLinks links = new FakeEmployeeLinks();
    private final FakeAcquiredCourses courses = new FakeAcquiredCourses();
    private final RecordingOrderAccessPort enrollments = new RecordingOrderAccessPort();
    private final AssignEmployeeCourseUseCase useCase = new AssignEmployeeCourseUseCase(
            links, courses, enrollments, CLOCK);

    @BeforeEach
    void setUp() {
        links.linkEmployee(COMPANY_ID, EMPLOYEE_ID, "Analista");
        courses.add(COURSE_ID, 30);
    }

    @Test
    void shouldEnrollTheEmployeeInTheCompanyCourse() {
        useCase.execute(COMPANY_ID, EMPLOYEE_ID, COURSE_ID);

        assertEquals(new Enrollment(TODAY, TODAY.plusDays(30)), enrollments.enrollment(EMPLOYEE_ID, COURSE_ID));
    }

    @Test
    void shouldRejectAnEmployeeOfAnotherCompany() {
        assertThrows(CompanyEmployeeNotFoundException.class,
                () -> useCase.execute(COMPANY_ID, UUID.randomUUID(), COURSE_ID));
    }

    @Test
    void shouldRejectACourseTheCompanyDoesNotOwn() {
        assertThrows(CourseNotFoundException.class,
                () -> useCase.execute(COMPANY_ID, EMPLOYEE_ID, UUID.randomUUID()));
    }

    @Test
    void shouldRejectNullArguments() {
        assertThrows(IllegalArgumentException.class,
                () -> useCase.execute(null, EMPLOYEE_ID, COURSE_ID));
        assertThrows(IllegalArgumentException.class,
                () -> useCase.execute(COMPANY_ID, null, COURSE_ID));
        assertThrows(IllegalArgumentException.class,
                () -> useCase.execute(COMPANY_ID, EMPLOYEE_ID, null));
    }

    @Test
    void shouldEnrollWithoutExpirationWhenCourseHasNoDeadline() {
        final UUID openCourse = UUID.randomUUID();
        courses.add(openCourse, null);

        useCase.execute(COMPANY_ID, EMPLOYEE_ID, openCourse);

        assertEquals(new Enrollment(TODAY, null), enrollments.enrollment(EMPLOYEE_ID, openCourse));
    }
}

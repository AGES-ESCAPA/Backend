package com.escapa.backend.application.usecase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.escapa.backend.application.model.EmployeeCourseAssignment;
import com.escapa.backend.domain.company.CompanyEmployeeNotFoundException;

class ListEmployeeCoursesUseCaseTest {

    private static final UUID COMPANY_ID = UUID.randomUUID();
    private static final UUID EMPLOYEE_ID = UUID.randomUUID();
    private static final UUID COURSE_ID = UUID.randomUUID();

    private final FakeEmployeeLinks links = new FakeEmployeeLinks();
    private final FakeAcquiredCourses courses = new FakeAcquiredCourses();
    private final ListEmployeeCoursesUseCase useCase = new ListEmployeeCoursesUseCase(links, courses);

    @BeforeEach
    void setUp() {
        links.linkEmployee(COMPANY_ID, EMPLOYEE_ID, "Analista");
        courses.add(COURSE_ID, 30);
    }

    @Test
    void shouldListAssignmentsForAnEmployeeOfTheCompany() {
        final List<EmployeeCourseAssignment> result = useCase.execute(COMPANY_ID, EMPLOYEE_ID);

        assertEquals(1, result.size());
        assertEquals(COURSE_ID, result.get(0).courseId());
    }

    @Test
    void shouldRejectAnEmployeeOfAnotherCompany() {
        assertThrows(CompanyEmployeeNotFoundException.class,
                () -> useCase.execute(COMPANY_ID, UUID.randomUUID()));
    }

    @Test
    void shouldRejectNullIds() {
        assertThrows(IllegalArgumentException.class, () -> useCase.execute(null, EMPLOYEE_ID));
        assertThrows(IllegalArgumentException.class, () -> useCase.execute(COMPANY_ID, null));
    }
}

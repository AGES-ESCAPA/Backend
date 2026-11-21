package com.escapa.backend.adapters.dto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.escapa.backend.application.dto.ChangeLogEntry;
import com.escapa.backend.application.model.CourseRules;

class CourseRulesResponseTest {

    @Test
    void shouldMapPrerequisitesAndChangeLogEntriesWithOptionalTimestamp() {
        final UUID prerequisiteId = UUID.randomUUID();
        final UUID changeId = UUID.randomUUID();
        final LocalDateTime changedAt = LocalDateTime.of(2026, 11, 17, 15, 30);
        final CourseRules rules = new CourseRules(
                true, false, "2.3",
                List.of(new CourseRules.Prerequisite(prerequisiteId, "Curso anterior")),
                List.of(
                        new ChangeLogEntry(changeId, "Regra alterada", "Admin", 2, 3, changedAt),
                        new ChangeLogEntry(UUID.randomUUID(), "Importado", "Sistema", 2, 2, null)));

        final CourseRulesResponse response = CourseRulesResponse.from(rules);

        assertEquals("2.3", response.version());
        assertEquals(new CourseRulesResponse.PrerequisiteResponse(prerequisiteId.toString(), "Curso anterior"),
                response.prerequisites().get(0));
        assertEquals(new CourseRulesResponse.ChangeLogResponse(
                changeId, "Regra alterada", "Admin", changedAt.toString()), response.changeLog().get(0));
        assertNull(response.changeLog().get(1).createdAt());
    }
}

package com.escapa.backend.domain.entity;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ModuleTest {

    @Test
    void shouldCountContentsAndSumOnlyKnownDurations() {
        final Module module = new Module();
        final Content withDuration = new Content();
        withDuration.setDurationMinutes(12);
        final Content withoutDuration = new Content();
        module.setContents(List.of(withDuration, withoutDuration));

        assertEquals(2, module.getTotalContents());
        assertEquals(12, module.getTotalDurationMinutes());
    }

    @Test
    void shouldReturnZeroForMissingContents() {
        final Module module = new Module();
        module.setContents(null);

        assertEquals(0, module.getTotalContents());
        assertEquals(0, module.getTotalDurationMinutes());
    }
}

package com.escapa.backend.application.dto;

import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

class OrderFilterTest {

    @Test
    void shouldIgnoreBlankSearchText() {
        final OrderFilter filter = new OrderFilter(null, null, null, null, null, "   ");

        assertNull(filter.search());
    }
}

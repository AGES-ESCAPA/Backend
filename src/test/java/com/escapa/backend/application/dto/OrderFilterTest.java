package com.escapa.backend.application.dto;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNull;

class OrderFilterTest {

    @Test
    void shouldIgnoreBlankSearchText() {
        final OrderFilter filter = new OrderFilter(null, null, null, null, null, "   ");

        assertNull(filter.search());
    }
}

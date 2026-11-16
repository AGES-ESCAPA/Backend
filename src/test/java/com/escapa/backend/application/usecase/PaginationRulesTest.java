package com.escapa.backend.application.usecase;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PaginationRulesTest {

    @Test
    void shouldAllowPagesOfOneUpToTheMaximumSize() {
        assertDoesNotThrow(() -> PaginationRules.requireValid(0, 1));
        assertDoesNotThrow(() -> PaginationRules.requireValid(7, 50));
        assertDoesNotThrow(() -> PaginationRules.requireValid(0, PaginationRules.MAX_PAGE_SIZE));
    }

    @ParameterizedTest
    @ValueSource(ints = {-1, -10})
    void shouldRefuseANegativePage(int page) {
        assertThrows(IllegalArgumentException.class, () -> PaginationRules.requireValid(page, 10));
    }

    @ParameterizedTest
    @ValueSource(ints = {0, -1, 101, 1000})
    void shouldRefuseASizeOutsideTheLimits(int size) {
        final IllegalArgumentException error =
                assertThrows(IllegalArgumentException.class, () -> PaginationRules.requireValid(0, size));

        assertEquals("Invalid pagination", error.getMessage());
    }

    @Test
    void shouldKeepTheMaximumSizeAtOneHundred() {
        assertEquals(100, PaginationRules.MAX_PAGE_SIZE);
    }
}

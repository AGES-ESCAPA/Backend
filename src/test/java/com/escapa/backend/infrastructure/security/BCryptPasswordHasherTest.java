package com.escapa.backend.infrastructure.security;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class BCryptPasswordHasherTest {

    private final BCryptPasswordHasher hasher = new BCryptPasswordHasher();

    @Test
    void shouldHashAndMatchTheSamePassword() {
        final String hash = hasher.hash("SenhaForte1!");

        assertTrue(hasher.matches("SenhaForte1!", hash));
        assertFalse(hasher.matches("outra-senha", hash));
        assertNotEquals("SenhaForte1!", hash);
    }

    @Test
    void shouldProduceDifferentHashesForTheSamePassword() {
        final String first = hasher.hash("SenhaForte1!");
        final String second = hasher.hash("SenhaForte1!");

        assertNotEquals(first, second);
        assertTrue(hasher.matches("SenhaForte1!", first));
        assertTrue(hasher.matches("SenhaForte1!", second));
    }
}

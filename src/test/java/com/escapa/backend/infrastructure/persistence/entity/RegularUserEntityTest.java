package com.escapa.backend.infrastructure.persistence.entity;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class RegularUserEntityTest {

    @Test
    void shouldMarkEntityAsExistingAfterPersistenceCallback() {
        final RegularUserEntity entity = new RegularUserEntity();

        assertTrue(entity.isNew());

        entity.markPersisted();

        assertFalse(entity.isNew());
    }
}

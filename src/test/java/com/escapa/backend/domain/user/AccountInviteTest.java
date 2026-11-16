package com.escapa.backend.domain.user;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AccountInviteTest {

    @Test
    void shouldHashTheTokenAsLowercaseSha256Hex() {
        // Vetor oficial do SHA-256 (FIPS 180-2). O seed grava o mesmo formato: encode(sha256(...), 'hex').
        assertEquals("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad",
                AccountInvite.hashToken("abc"));
    }
}

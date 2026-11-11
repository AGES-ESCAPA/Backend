package com.escapa.backend.application.usecase;

import com.escapa.backend.application.port.ContactValidatorPort;

/** Valida e-mail de forma simples: texto@texto.texto, sem espaços. */
public final class FakeContactValidator implements ContactValidatorPort {

    @Override
    public boolean isValidEmail(String email) {
        return email != null && email.matches("[^@\\s]+@[^@\\s]+\\.[^@\\s]+");
    }

    @Override
    public String normalizePhone(String phone) {
        throw new UnsupportedOperationException();
    }
}

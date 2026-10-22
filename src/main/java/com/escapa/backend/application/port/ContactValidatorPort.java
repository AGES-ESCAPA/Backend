package com.escapa.backend.application.port;

/** Validacao de contatos sem expor bibliotecas externas aos casos de uso. */
public interface ContactValidatorPort {

    boolean isValidEmail(String email);

    /** Retorna o telefone com apenas digitos, ou null quando ausente; rejeita formatos invalidos. */
    String normalizePhone(String phone);
}

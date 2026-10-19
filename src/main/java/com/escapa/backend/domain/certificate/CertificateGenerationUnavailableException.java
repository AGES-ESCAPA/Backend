package com.escapa.backend.domain.certificate;

/**
 * O gerador de PDF esta ocupado (fila cheia) ou nao respondeu dentro do prazo.
 * Vira 503: o cliente pode tentar de novo em instantes.
 */
public class CertificateGenerationUnavailableException extends RuntimeException {

    public CertificateGenerationUnavailableException(String message) {
        super(message);
    }
}

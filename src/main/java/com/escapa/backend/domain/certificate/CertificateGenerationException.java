package com.escapa.backend.domain.certificate;

/** Falha ao gerar o PDF do certificado. Vira 500 com mensagem generica; a causa so vai para o log. */
public class CertificateGenerationException extends RuntimeException {

    public CertificateGenerationException(String message, Throwable cause) {
        super(message, cause);
    }
}

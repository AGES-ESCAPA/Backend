package com.escapa.backend.domain.certificate;

/** O usuario autenticado nao e o dono do certificado solicitado. */
public class CertificateAccessDeniedException extends RuntimeException {

    public CertificateAccessDeniedException() {
        super("Access denied: user is not the certificate owner");
    }
}

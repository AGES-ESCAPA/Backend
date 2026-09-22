package com.escapa.backend.domain.certificate;

/**
 * O codigo de verificacao nao corresponde a nenhum certificado emitido: nao
 * existe, ou a matricula vinculada ainda nao concluiu o curso.
 */
public class CertificateNotFoundException extends RuntimeException {

    public CertificateNotFoundException(String verificationCode) {
        super("Certificate not found: " + verificationCode);
    }
}

package com.escapa.backend.domain.certificate;

/**
 * O codigo de verificacao nao corresponde a nenhum certificado emitido: nao
 * existe, ou a matricula vinculada ainda nao concluiu o curso.
 *
 * A mensagem nao leva o codigo porque ela vai para o log do tratador global.
 */
public class CertificateNotFoundException extends RuntimeException {

    public CertificateNotFoundException() {
        super("Certificate not found");
    }
}

package com.escapa.backend.application.port;

import com.escapa.backend.application.model.FormattedCertificate;
import com.escapa.backend.domain.certificate.CertificateTheme;

/** Gera o PDF do certificado a partir do template da tela. Implementado na infraestrutura. */
public interface CertificatePdfGeneratorPort {

    /**
     * Versao do template visual. Entra na chave do cache: trocar o template e
     * incrementar a versao faz os PDFs antigos deixarem de ser servidos.
     */
    String templateVersion();

    byte[] generate(FormattedCertificate certificate, CertificateTheme theme);
}

package com.escapa.backend.application.port;

import com.escapa.backend.application.model.CertificateRecord;

/** Gera o PDF do certificado a partir do template fixo. Implementado na infraestrutura. */
public interface CertificatePdfGeneratorPort {

    byte[] generate(CertificateRecord certificate);
}

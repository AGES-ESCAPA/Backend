package com.escapa.backend.application.port;

import com.escapa.backend.application.model.CertificateRecord;

import java.util.Optional;
import java.util.UUID;

public interface CertificateRepositoryPort {

    /**
     * Certificado pelo codigo de verificacao publico.
     *
     * @return vazio se o codigo nao existir ou a matricula nao tiver certificado emitido
     */
    Optional<CertificateRecord> findByVerificationCode(String verificationCode);

    /** Guarda o PDF gerado para reuso nos proximos downloads, sem reprocessar o template. */
    void saveCachedPdf(UUID userId, UUID courseId, byte[] pdf);
}

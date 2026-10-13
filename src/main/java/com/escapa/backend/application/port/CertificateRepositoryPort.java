package com.escapa.backend.application.port;

import com.escapa.backend.application.model.CertificateDetails;
import com.escapa.backend.domain.certificate.CertificateTheme;

import java.util.Optional;

public interface CertificateRepositoryPort {

    /**
     * Dados da tela de certificado (US-18), com aluno, curso e avaliacoes numa
     * unica consulta. O download (US-19) usa a mesma consulta.
     *
     * @return vazio se o codigo nao existir ou a matricula nao tiver certificado emitido
     */
    Optional<CertificateDetails> findDetailsByVerificationCode(String verificationCode);

    /** PDF ja gerado para o codigo, tema e versao do template, se existir. */
    Optional<byte[]> findCachedPdf(String verificationCode, CertificateTheme theme, String templateVersion);

    /**
     * Guarda o PDF gerado e descarta os do mesmo codigo e tema gerados com
     * versoes anteriores do template.
     */
    void saveCachedPdf(String verificationCode, CertificateTheme theme, String templateVersion, byte[] pdf);
}

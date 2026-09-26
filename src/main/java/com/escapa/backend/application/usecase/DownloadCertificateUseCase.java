package com.escapa.backend.application.usecase;

import com.escapa.backend.application.model.CertificateFile;
import com.escapa.backend.application.model.CertificateRecord;
import com.escapa.backend.application.port.CertificatePdfGeneratorPort;
import com.escapa.backend.application.port.CertificateRepositoryPort;
import com.escapa.backend.domain.certificate.CertificateNotFoundException;

import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Devolve o PDF do certificado para um codigo de verificacao (US-19). O
 * arquivo e gerado na primeira vez e reaproveitado nos downloads seguintes.
 */
public class DownloadCertificateUseCase {

    private static final Pattern DIACRITICS = Pattern.compile("\\p{M}");
    private static final Pattern NON_ALPHANUMERIC = Pattern.compile("[^a-z0-9]+");
    private static final Pattern EDGE_HYPHENS = Pattern.compile("(^-|-$)");

    private final CertificateRepositoryPort certificateRepositoryPort;
    private final CertificatePdfGeneratorPort certificatePdfGeneratorPort;

    public DownloadCertificateUseCase(
            CertificateRepositoryPort certificateRepositoryPort,
            CertificatePdfGeneratorPort certificatePdfGeneratorPort
    ) {
        this.certificateRepositoryPort = certificateRepositoryPort;
        this.certificatePdfGeneratorPort = certificatePdfGeneratorPort;
    }

    public CertificateFile execute(String verificationCode) {
        // "nao existir" e "curso nao concluido" viram o mesmo 404: nenhum dos
        // dois revela ao chamador qual das duas condicoes falhou.
        final CertificateRecord certificate = certificateRepositoryPort.findByVerificationCode(verificationCode)
                .filter(record -> record.conclusionDate() != null)
                .orElseThrow(() -> new CertificateNotFoundException(verificationCode));

        final byte[] pdf = certificate.cachedPdf() != null
                ? certificate.cachedPdf()
                : generateAndCache(certificate);

        return new CertificateFile(pdf, toFilename(certificate.courseTitle()));
    }

    private byte[] generateAndCache(CertificateRecord certificate) {
        final byte[] pdf = certificatePdfGeneratorPort.generate(certificate);
        certificateRepositoryPort.saveCachedPdf(certificate.userId(), certificate.courseId(), pdf);
        return pdf;
    }

    /** "Marketing Digital para Hospitalidade" vira "certificado-marketing-digital-para-hospitalidade.pdf". */
    private String toFilename(String courseTitle) {
        final String normalized = Normalizer.normalize(courseTitle, Normalizer.Form.NFD);
        final String withoutDiacritics = DIACRITICS.matcher(normalized).replaceAll("");
        final String slug = NON_ALPHANUMERIC.matcher(withoutDiacritics.toLowerCase(Locale.ROOT)).replaceAll("-");
        return "certificado-" + EDGE_HYPHENS.matcher(slug).replaceAll("") + ".pdf";
    }
}

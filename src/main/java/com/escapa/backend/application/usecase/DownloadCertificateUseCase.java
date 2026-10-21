package com.escapa.backend.application.usecase;

import com.escapa.backend.application.model.CertificateDetails;
import com.escapa.backend.application.model.CertificateFile;
import com.escapa.backend.application.model.FormattedCertificate;
import com.escapa.backend.application.port.CertificatePdfGeneratorPort;
import com.escapa.backend.application.port.CertificateRepositoryPort;
import com.escapa.backend.domain.certificate.CertificateTheme;

import java.text.Normalizer;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.regex.Pattern;

/**
 * Devolve o PDF do certificado para um codigo de verificacao e um tema (US-19).
 * Os dados vem do mesmo caso de uso da tela (US-18), e o arquivo e gerado na
 * primeira vez e reaproveitado do cache nos downloads seguintes.
 */
public class DownloadCertificateUseCase {

    private static final Pattern DIACRITICS = Pattern.compile("\\p{M}");
    private static final Pattern NON_ALPHANUMERIC = Pattern.compile("[^a-z0-9]+");
    private static final Pattern EDGE_HYPHENS = Pattern.compile("(^-|-$)");

    private final GetCertificateDetailsUseCase getCertificateDetailsUseCase;
    private final CertificateRepositoryPort certificateRepositoryPort;
    private final CertificatePdfGeneratorPort certificatePdfGeneratorPort;

    // Geracoes em andamento por chave (codigo + tema + versao): quem chega durante
    // a primeira geracao espera o mesmo resultado em vez de gerar de novo. Vale por
    // instancia da aplicacao; com varias instancias, uma geracao duplicada rara
    // produz o mesmo arquivo.
    private final ConcurrentMap<String, CompletableFuture<byte[]>> inFlight = new ConcurrentHashMap<>();

    public DownloadCertificateUseCase(
            GetCertificateDetailsUseCase getCertificateDetailsUseCase,
            CertificateRepositoryPort certificateRepositoryPort,
            CertificatePdfGeneratorPort certificatePdfGeneratorPort
    ) {
        this.getCertificateDetailsUseCase = getCertificateDetailsUseCase;
        this.certificateRepositoryPort = certificateRepositoryPort;
        this.certificatePdfGeneratorPort = certificatePdfGeneratorPort;
    }

    public CertificateFile execute(String verificationCode, CertificateTheme theme) {
        // Antes do cache: o 404 vale mesmo que ja exista um PDF salvo para o codigo.
        final CertificateDetails details = getCertificateDetailsUseCase.execute(verificationCode).details();
        final String templateVersion = certificatePdfGeneratorPort.templateVersion();

        final byte[] pdf = certificateRepositoryPort
                .findCachedPdf(verificationCode, theme, templateVersion)
                .orElseGet(() -> generateOnce(details, theme, templateVersion));

        return new CertificateFile(pdf, toFilename(details.course().title()));
    }

    private byte[] generateOnce(CertificateDetails details, CertificateTheme theme, String templateVersion) {
        final String key = details.verificationCode() + "|" + theme + "|" + templateVersion;
        final CompletableFuture<byte[]> mine = new CompletableFuture<>();
        final CompletableFuture<byte[]> running = inFlight.putIfAbsent(key, mine);
        if (running != null) {
            return await(running);
        }

        try {
            // Dupla checagem: outra requisicao pode ter salvo o PDF entre a primeira
            // consulta ao cache e a reserva da chave.
            final Optional<byte[]> cached = certificateRepositoryPort
                    .findCachedPdf(details.verificationCode(), theme, templateVersion);
            final byte[] pdf = cached.orElseGet(() -> generateAndCache(details, theme, templateVersion));
            mine.complete(pdf);
            return pdf;
        } catch (RuntimeException e) {
            mine.completeExceptionally(e);
            throw e;
        } finally {
            inFlight.remove(key, mine);
        }
    }

    private byte[] generateAndCache(CertificateDetails details, CertificateTheme theme, String templateVersion) {
        // So salva depois de gerar com sucesso: uma falha propaga sem deixar nada no cache.
        final byte[] pdf = certificatePdfGeneratorPort.generate(FormattedCertificate.from(details), theme);
        certificateRepositoryPort.saveCachedPdf(details.verificationCode(), theme, templateVersion, pdf);
        return pdf;
    }

    private static byte[] await(CompletableFuture<byte[]> running) {
        try {
            return running.join();
        } catch (CompletionException e) {
            if (e.getCause() instanceof RuntimeException cause) {
                throw cause;
            }
            throw e;
        }
    }

    /** "Marketing Digital para Hospitalidade" vira "certificado-marketing-digital-para-hospitalidade.pdf". */
    private String toFilename(String courseTitle) {
        final String normalized = Normalizer.normalize(courseTitle, Normalizer.Form.NFD);
        final String withoutDiacritics = DIACRITICS.matcher(normalized).replaceAll("");
        final String slug = NON_ALPHANUMERIC.matcher(withoutDiacritics.toLowerCase(Locale.ROOT)).replaceAll("-");
        return "certificado-" + EDGE_HYPHENS.matcher(slug).replaceAll("") + ".pdf";
    }
}

package com.escapa.backend.application.usecase;

import com.escapa.backend.application.model.FormattedCertificate;
import com.escapa.backend.application.port.CertificatePdfGeneratorPort;
import com.escapa.backend.domain.certificate.CertificateTheme;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Nao chama nenhum navegador: devolve bytes previsiveis e conta as chamadas.
 * Pode falhar de proposito ou segurar a geracao ate um latch ser liberado, para
 * os testes de concorrencia.
 */
public final class FakeCertificatePdfGeneratorPort implements CertificatePdfGeneratorPort {
    private final AtomicInteger calls = new AtomicInteger();
    private String templateVersion = "v2";
    private RuntimeException failure;
    private CountDownLatch release;

    @Override
    public String templateVersion() {
        return templateVersion;
    }

    @Override
    public byte[] generate(FormattedCertificate certificate, CertificateTheme theme) {
        calls.incrementAndGet();
        if (release != null) {
            await(release);
        }
        if (failure != null) {
            throw failure;
        }
        return pdfFor(certificate.verificationCode(), theme, templateVersion);
    }

    static byte[] pdfFor(String verificationCode, CertificateTheme theme, String templateVersion) {
        return ("pdf-of-" + verificationCode + "-" + theme + "-" + templateVersion)
                .getBytes(StandardCharsets.UTF_8);
    }

    int calls() {
        return calls.get();
    }

    void useTemplateVersion(String version) {
        this.templateVersion = version;
    }

    void failWith(RuntimeException error) {
        this.failure = error;
    }

    void holdUntil(CountDownLatch latch) {
        this.release = latch;
    }

    private static void await(CountDownLatch latch) {
        try {
            if (!latch.await(5, TimeUnit.SECONDS)) {
                throw new IllegalStateException("Test latch was never released");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        }
    }
}

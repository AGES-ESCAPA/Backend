package com.escapa.backend.application.usecase;

import com.escapa.backend.application.model.CertificateDetails;
import com.escapa.backend.application.port.CertificateRepositoryPort;
import com.escapa.backend.domain.certificate.CertificateTheme;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public final class InMemoryCertificateRepositoryPort implements CertificateRepositoryPort {
    private final Map<String, CertificateDetails> detailsByCode = new ConcurrentHashMap<>();
    private final Map<String, byte[]> pdfs = new ConcurrentHashMap<>();

    public void addDetails(CertificateDetails details) {
        detailsByCode.put(details.verificationCode(), details);
    }

    @Override
    public Optional<CertificateDetails> findDetailsByVerificationCode(String verificationCode) {
        return Optional.ofNullable(detailsByCode.get(verificationCode));
    }

    @Override
    public Optional<byte[]> findCachedPdf(String verificationCode, CertificateTheme theme, String templateVersion) {
        return Optional.ofNullable(pdfs.get(key(verificationCode, theme, templateVersion)));
    }

    @Override
    public void saveCachedPdf(String verificationCode, CertificateTheme theme, String templateVersion, byte[] pdf) {
        // Mesmo contrato do adapter real: grava o novo e descarta as versoes antigas do mesmo codigo e tema.
        pdfs.keySet().removeIf(existing -> existing.startsWith(verificationCode + "|" + theme + "|"));
        pdfs.put(key(verificationCode, theme, templateVersion), pdf);
    }

    int cachedPdfCount() {
        return pdfs.size();
    }

    private static String key(String verificationCode, CertificateTheme theme, String templateVersion) {
        return verificationCode + "|" + theme + "|" + templateVersion;
    }
}

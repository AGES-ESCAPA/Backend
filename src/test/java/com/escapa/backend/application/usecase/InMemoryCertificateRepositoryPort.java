package com.escapa.backend.application.usecase;

import com.escapa.backend.application.model.CertificateDetails;
import com.escapa.backend.application.model.CertificateRecord;
import com.escapa.backend.application.port.CertificateRepositoryPort;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public final class InMemoryCertificateRepositoryPort implements CertificateRepositoryPort {
    private final Map<String, CertificateRecord> byCode = new HashMap<>();
    private final Map<String, CertificateDetails> detailsByCode = new HashMap<>();

    public void add(CertificateRecord certificate) {
        byCode.put(certificate.verificationCode(), certificate);
    }

    public void addDetails(CertificateDetails details) {
        detailsByCode.put(details.verificationCode(), details);
    }

    @Override
    public Optional<CertificateRecord> findByVerificationCode(String verificationCode) {
        return Optional.ofNullable(byCode.get(verificationCode));
    }

    @Override
    public Optional<CertificateDetails> findDetailsByVerificationCode(String verificationCode) {
        return Optional.ofNullable(detailsByCode.get(verificationCode));
    }

    @Override
    public void saveCachedPdf(UUID userId, UUID courseId, byte[] pdf) {
        byCode.replaceAll((code, record) -> record.userId().equals(userId) && record.courseId().equals(courseId)
                ? new CertificateRecord(
                        record.userId(), record.courseId(), record.studentName(), record.courseTitle(),
                        record.workloadMinutes(), record.conclusionDate(), record.verificationCode(), pdf)
                : record);
    }
}

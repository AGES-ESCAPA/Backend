package com.escapa.backend.application.usecase;

import com.escapa.backend.application.model.CertificateRecord;
import com.escapa.backend.application.port.CertificateRepositoryPort;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

final class InMemoryCertificateRepositoryPort implements CertificateRepositoryPort {
    private final Map<String, CertificateRecord> byCode = new HashMap<>();

    void add(CertificateRecord certificate) {
        byCode.put(certificate.verificationCode(), certificate);
    }

    @Override
    public Optional<CertificateRecord> findByVerificationCode(String verificationCode) {
        return Optional.ofNullable(byCode.get(verificationCode));
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

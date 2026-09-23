package com.escapa.backend.infrastructure.persistence;

import com.escapa.backend.application.model.CertificateRecord;
import com.escapa.backend.application.port.CertificateRepositoryPort;
import com.escapa.backend.infrastructure.persistence.entity.CourseEntity;
import com.escapa.backend.infrastructure.persistence.entity.UserCourseEntity;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Repository
public class CertificateRepositoryAdapter implements CertificateRepositoryPort {

    private final UserCourseJpaRepository userCourseJpaRepository;

    public CertificateRepositoryAdapter(UserCourseJpaRepository userCourseJpaRepository) {
        this.userCourseJpaRepository = userCourseJpaRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<CertificateRecord> findByVerificationCode(String verificationCode) {
        return userCourseJpaRepository.findIssuedByCertificateCode(verificationCode)
                .map(entity -> toRecord(entity, verificationCode));
    }

    @Override
    @Transactional
    public void saveCachedPdf(UUID userId, UUID courseId, byte[] pdf) {
        userCourseJpaRepository.updateCertificatePdf(userId, courseId, pdf);
    }

    private CertificateRecord toRecord(UserCourseEntity entity, String verificationCode) {
        final CourseEntity course = entity.getCourse();
        // Busca isolada do PDF em cache (ver UserCourseEntity): so ele carrega o BYTEA,
        // nunca o SELECT acima nem qualquer outro load de UserCourseEntity.
        final byte[] cachedPdf = userCourseJpaRepository
                .findCachedCertificatePdfByCertificateCode(verificationCode)
                .orElse(null);
        return new CertificateRecord(
                entity.getId().getUserId(),
                entity.getId().getCourseId(),
                entity.getUser().getName(),
                course.getTitle(),
                course.getDurationTime(),
                entity.getConclusionDate(),
                entity.getCertificateCode(),
                cachedPdf
        );
    }
}

package com.escapa.backend.infrastructure.persistence;

import com.escapa.backend.application.model.CertificateDetails;
import com.escapa.backend.application.port.CertificateRepositoryPort;
import com.escapa.backend.domain.certificate.CertificateTheme;
import com.escapa.backend.infrastructure.persistence.UserCourseJpaRepository.CertificateDetailsView;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Repository
public class CertificateRepositoryAdapter implements CertificateRepositoryPort {

    private final UserCourseJpaRepository userCourseJpaRepository;
    // O cache de PDFs (certificate_pdfs) nao tem entidade JPA: so e lido e gravado
    // aqui, por SQL direto, e assim o BYTEA nunca e carregado junto de outra entidade.
    private final JdbcClient jdbcClient;

    public CertificateRepositoryAdapter(UserCourseJpaRepository userCourseJpaRepository, JdbcClient jdbcClient) {
        this.userCourseJpaRepository = userCourseJpaRepository;
        this.jdbcClient = jdbcClient;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<CertificateDetails> findDetailsByVerificationCode(String verificationCode) {
        return userCourseJpaRepository.findIssuedDetailsByCertificateCode(verificationCode)
                .map(this::toDetails);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<byte[]> findCachedPdf(String verificationCode, CertificateTheme theme, String templateVersion) {
        return jdbcClient.sql("select pdf from certificate_pdfs "
                        + "where certificate_code = :code and theme = :theme and template_version = :version")
                .param("code", verificationCode)
                .param("theme", theme.name())
                .param("version", templateVersion)
                .query(byte[].class)
                .optional();
    }

    @Override
    @Transactional
    public void saveCachedPdf(String verificationCode, CertificateTheme theme, String templateVersion, byte[] pdf) {
        // ON CONFLICT: com varias instancias, duas podem gerar o mesmo PDF ao mesmo
        // tempo; o segundo insert e ignorado em vez de falhar o download.
        jdbcClient.sql("insert into certificate_pdfs (certificate_code, theme, template_version, pdf) "
                        + "values (:code, :theme, :version, :pdf) on conflict do nothing")
                .param("code", verificationCode)
                .param("theme", theme.name())
                .param("version", templateVersion)
                .param("pdf", pdf)
                .update();
        // PDFs do mesmo certificado e tema gerados por templates anteriores nao
        // serao mais servidos: sai o lixo junto com a gravacao do novo.
        jdbcClient.sql("delete from certificate_pdfs "
                        + "where certificate_code = :code and theme = :theme and template_version <> :version")
                .param("code", verificationCode)
                .param("theme", theme.name())
                .param("version", templateVersion)
                .update();
    }

    private CertificateDetails toDetails(CertificateDetailsView view) {
        final CertificateDetails.Student student = new CertificateDetails.Student(
                view.getStudentId(),
                view.getStudentName(),
                view.getStudentAvatarUrl(),
                view.isStudentVerified()
        );
        final CertificateDetails.Course course = new CertificateDetails.Course(
                view.getCourseId(),
                view.getCourseTitle(),
                view.getCourseDescription(),
                view.getCourseCategory(),
                view.getCourseLevel(),
                view.getCourseThumbnailUrl(),
                view.getCourseDurationTime(),
                view.getCourseLessonsCount(),
                view.getCourseRating(),
                view.getCourseReviewsCount(),
                view.getInstructorName(),
                view.getCoursePrice()
        );
        return new CertificateDetails(
                view.getConclusionDate(),
                view.getCourseDurationTime(),
                view.getVerificationCode(),
                student,
                course
        );
    }
}

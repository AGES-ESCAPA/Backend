package com.escapa.backend.application.usecase;

import com.escapa.backend.application.model.CertificateDetails;
import com.escapa.backend.application.model.CertificateView;
import com.escapa.backend.application.port.CertificateRepositoryPort;
import com.escapa.backend.domain.certificate.CertificateNotFoundException;

/**
 * Dados da tela de certificado digital pelo codigo de verificacao (US-18).
 * Publico nesta sprint, como o download da US-19.
 */
public class GetCertificateDetailsUseCase {

    private final CertificateRepositoryPort certificateRepositoryPort;

    public GetCertificateDetailsUseCase(CertificateRepositoryPort certificateRepositoryPort) {
        this.certificateRepositoryPort = certificateRepositoryPort;
    }

    public CertificateView execute(String verificationCode) {
        // Mesma regra do download: "nao existe" e "curso nao concluido" viram o
        // mesmo 404, sem revelar qual das duas condicoes falhou.
        final CertificateDetails details = certificateRepositoryPort.findDetailsByVerificationCode(verificationCode)
                .filter(certificate -> certificate.conclusionDate() != null)
                .orElseThrow(() -> new CertificateNotFoundException(verificationCode));

        // A consulta por codigo e publica; por isso ela nao revela a identidade
        // autenticada nem marca a visualizacao como sendo do dono.
        return new CertificateView(details, false);
    }
}

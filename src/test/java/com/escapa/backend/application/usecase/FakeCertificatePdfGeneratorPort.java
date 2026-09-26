package com.escapa.backend.application.usecase;

import com.escapa.backend.application.model.CertificateRecord;
import com.escapa.backend.application.port.CertificatePdfGeneratorPort;

import java.nio.charset.StandardCharsets;

/** Nao chama nenhuma biblioteca de PDF: devolve bytes previsiveis e conta as chamadas. */
final class FakeCertificatePdfGeneratorPort implements CertificatePdfGeneratorPort {
    private int calls;

    @Override
    public byte[] generate(CertificateRecord certificate) {
        calls++;
        return ("pdf-of-" + certificate.verificationCode()).getBytes(StandardCharsets.UTF_8);
    }

    int calls() {
        return calls;
    }
}

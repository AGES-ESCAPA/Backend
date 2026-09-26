package com.escapa.backend.adapters.controller;

import com.escapa.backend.application.model.CertificateFile;
import com.escapa.backend.application.usecase.DownloadCertificateUseCase;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Download do certificado do aluno em PDF (US-19). Publico nesta sprint: o
 * {@code verificationCode} e o unico controle de acesso, ja que a validacao
 * de dono do certificado depende da autenticacao ainda nao implementada.
 */
@RestController
@RequestMapping("/api/v1/certificates")
public class CertificateController {

    private final DownloadCertificateUseCase downloadCertificateUseCase;

    public CertificateController(DownloadCertificateUseCase downloadCertificateUseCase) {
        this.downloadCertificateUseCase = downloadCertificateUseCase;
    }

    @GetMapping("/{verificationCode}/download")
    public ResponseEntity<byte[]> download(@PathVariable String verificationCode) {
        final CertificateFile file = downloadCertificateUseCase.execute(verificationCode);
        final ContentDisposition disposition = ContentDisposition.attachment()
                .filename(file.filename())
                .build();

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .body(file.content());
    }
}

package com.escapa.backend.adapters.controller;

import com.escapa.backend.adapters.dto.certificate.CertificateResponseDTO;
import com.escapa.backend.application.model.CertificateFile;
import com.escapa.backend.application.service.CertificateService;
import com.escapa.backend.application.usecase.DownloadCertificateUseCase;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/certificates")
public class CertificateController {

    private final CertificateService certificateService;
    private final DownloadCertificateUseCase downloadCertificateUseCase;

    @Autowired
    public CertificateController(
            CertificateService certificateService,
            DownloadCertificateUseCase downloadCertificateUseCase
    ) {
        this.certificateService = certificateService;
        this.downloadCertificateUseCase = downloadCertificateUseCase;
    }

    public CertificateController(DownloadCertificateUseCase downloadCertificateUseCase) {
        this(null, downloadCertificateUseCase);
    }

    @GetMapping("/{verificationCode}")
    public ResponseEntity<CertificateResponseDTO> getCertificateByCode(
            @PathVariable String verificationCode
    ) {
        final CertificateResponseDTO response = certificateService.getCertificateByCode(verificationCode);
        return ResponseEntity.ok(response);
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

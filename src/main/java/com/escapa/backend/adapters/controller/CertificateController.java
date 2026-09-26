package com.escapa.backend.adapters.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.escapa.backend.adapters.dto.certificate.CertificateResponseDTO;
import com.escapa.backend.application.service.CertificateService;

@RestController
@RequestMapping("/api/v1/certificates")
public class CertificateController {
    private final CertificateService certificateService;

    public CertificateController(CertificateService certificateService) {
        this.certificateService = certificateService;
    }

    @GetMapping("/{verificationCode}")
    public ResponseEntity<CertificateResponseDTO> getCertificateByCode(
            @PathVariable String verificationCode
    ) {
        final CertificateResponseDTO response = certificateService.getCertificateByCode(verificationCode);
        return ResponseEntity.ok(response);
    }
}

package com.escapa.backend.adapters.controller;

import com.escapa.backend.adapters.dto.ApiResponse;
import com.escapa.backend.adapters.dto.CertificateDetailsResponse;
import com.escapa.backend.adapters.exception.ApiError;
import com.escapa.backend.adapters.security.AuthenticatedUser;
import com.escapa.backend.adapters.security.AuthenticationContext;
import com.escapa.backend.application.model.CertificateFile;
import com.escapa.backend.application.usecase.DownloadCertificateUseCase;
import com.escapa.backend.application.usecase.GetCertificateDetailsUseCase;
import com.escapa.backend.domain.certificate.CertificateTheme;
import java.util.UUID;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * A consulta por codigo de verificacao e publica. O download do PDF exige o token
 * do dono do certificado.
 */
@RestController
@Tag(name = "Certificates", description = "Certificate lookup and PDF download by verification code")
@RequestMapping("/api/v1/certificates")
public class CertificateController {

    private final GetCertificateDetailsUseCase getCertificateDetailsUseCase;
    private final DownloadCertificateUseCase downloadCertificateUseCase;
    private final CertificateTheme defaultTheme;

    public CertificateController(
            GetCertificateDetailsUseCase getCertificateDetailsUseCase,
            DownloadCertificateUseCase downloadCertificateUseCase,
            @Value("${certificate.default-theme}") String defaultTheme
    ) {
        this.getCertificateDetailsUseCase = getCertificateDetailsUseCase;
        this.downloadCertificateUseCase = downloadCertificateUseCase;
        this.defaultTheme = CertificateTheme.fromParam(defaultTheme);
    }

    @GetMapping("/{verificationCode}")
    @Operation(
            summary = "Get certificate details by verification code",
            description = "Public. With a valid Bearer token of the certificate owner, isOwner is true.")
    public ResponseEntity<ApiResponse<CertificateDetailsResponse>> getByVerificationCode(
            @PathVariable String verificationCode
    ) {
        final UUID viewerId = AuthenticationContext.currentUser().map(AuthenticatedUser::id).orElse(null);
        return ResponseEntity.ok(ApiResponse.success(
                CertificateDetailsResponse.from(getCertificateDetailsUseCase.execute(verificationCode, viewerId))));
    }

    @GetMapping("/{verificationCode}/download")
    @Operation(
            summary = "Download the certificate PDF",
            description = "Requires the Bearer token of the certificate owner.",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Certificate PDF",
                    content = @Content(mediaType = MediaType.APPLICATION_PDF_VALUE,
                            schema = @Schema(type = "string", format = "binary"))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400",
                    description = "Invalid theme (use light or dark)",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401",
                    description = "Missing, malformed or expired Bearer token",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403",
                    description = "Authenticated user is not the certificate owner",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404",
                    description = "Verification code does not exist or the course is not concluded",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    public ResponseEntity<byte[]> download(
            @PathVariable String verificationCode,
            @RequestParam(required = false) String theme
    ) {
        // Sem o parametro vale o tema padrao (certificate.default-theme); valor
        // diferente de light/dark vira 400 pelo IllegalArgumentException.
        final CertificateTheme selectedTheme = theme == null ? defaultTheme : CertificateTheme.fromParam(theme);
        final CertificateFile file = downloadCertificateUseCase.execute(
                AuthenticationContext.requireUser().id(), verificationCode, selectedTheme);
        final ContentDisposition disposition = ContentDisposition.attachment()
                .filename(file.filename())
                .build();

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .body(file.content());
    }
}

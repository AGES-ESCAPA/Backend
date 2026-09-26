
package com.escapa.backend.adapters.dto.certificate;

import java.time.LocalDate;

public class CertificateSummaryDTO {
    private LocalDate conclusionDate;
    private Integer workload;
    private String verificationCode;

    public CertificateSummaryDTO(LocalDate conclusionDate, String verificationCode, Integer workload) {
        this.conclusionDate = conclusionDate;
        this.verificationCode = verificationCode;
        this.workload = workload;
    }

    public LocalDate getConclusionDate() {
        return conclusionDate;
    }

    public Integer getWorkload() {
        return workload;
    }

    public String getVerificationCode() {
        return verificationCode;
    }
}

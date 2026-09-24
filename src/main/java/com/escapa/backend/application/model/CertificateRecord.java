package com.escapa.backend.application.model;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Dados do certificado emitido para uma matricula, o suficiente para montar o
 * PDF (US-19) com os mesmos campos exibidos na tela de certificado (US-18).
 *
 * {@code cachedPdf} e o arquivo ja gerado em um download anterior, ou
 * {@code null} na primeira vez. Como o conteudo binario nunca e comparado
 * entre instancias, o {@code equals}/{@code toString} gerados pelo record
 * (por referencia para arrays) nao chega a ser um problema aqui.
 */
public record CertificateRecord(
        UUID userId,
        UUID courseId,
        String studentName,
        String courseTitle,
        /** Carga horaria do curso, em minutos (Course.durationTime). */
        Integer workloadMinutes,
        LocalDate conclusionDate,
        String verificationCode,
        byte[] cachedPdf
) {
}

package com.escapa.backend.application.model;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Textos do certificado ja formatados para exibicao. E o unico lugar da
 * formatacao: o PDF (US-19) usa este record, e a tela (US-18) recebe os mesmos
 * textos pela API, para os dois nunca divergirem.
 */
public record FormattedCertificate(
        String studentName,
        String courseTitle,
        /** Ex.: "21 de agosto de 2025". */
        String conclusionDate,
        /** Ex.: "16 horas", "1 hora", "1 hora e 30 minutos". */
        String workload,
        /** Exibido exatamente como esta salvo, ex.: "ESC-21AGO25-7X9L2M3N". */
        String verificationCode,
        /** Ex.: "escapa.com.br/certificados/ESC-21AGO25-7X9L2M3N". */
        String validationUrl
) {

    private static final DateTimeFormatter CONCLUSION_DATE_FORMAT =
            DateTimeFormatter.ofPattern("d 'de' MMMM 'de' yyyy", Locale.of("pt", "BR"));
    private static final String VALIDATION_URL_PREFIX = "escapa.com.br/certificados/";
    private static final int MINUTES_PER_HOUR = 60;

    public static FormattedCertificate from(CertificateDetails details) {
        return new FormattedCertificate(
                details.student().name(),
                details.course().title(),
                formatConclusionDate(details.conclusionDate()),
                formatWorkload(details.workloadMinutes()),
                details.verificationCode(),
                validationUrl(details.verificationCode())
        );
    }

    public static String formatConclusionDate(LocalDate conclusionDate) {
        return conclusionDate.format(CONCLUSION_DATE_FORMAT);
    }

    public static String formatWorkload(Integer workloadMinutes) {
        if (workloadMinutes == null || workloadMinutes <= 0) {
            return "0 horas";
        }
        final int hours = workloadMinutes / MINUTES_PER_HOUR;
        final int minutes = workloadMinutes % MINUTES_PER_HOUR;
        if (minutes == 0) {
            return plural(hours, "hora", "horas");
        }
        if (hours == 0) {
            return plural(minutes, "minuto", "minutos");
        }
        return plural(hours, "hora", "horas") + " e " + plural(minutes, "minuto", "minutos");
    }

    public static String validationUrl(String verificationCode) {
        return VALIDATION_URL_PREFIX + verificationCode;
    }

    private static String plural(int amount, String singular, String pluralForm) {
        return amount + " " + (amount == 1 ? singular : pluralForm);
    }
}

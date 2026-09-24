package com.escapa.backend.infrastructure.pdf;

import com.escapa.backend.application.model.CertificateRecord;
import com.escapa.backend.application.port.CertificatePdfGeneratorPort;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfWriter;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Gera o PDF do certificado (US-19) com o template fixo: nome do aluno, curso,
 * carga horaria, data de conclusao e codigo de verificacao — os mesmos campos
 * exibidos na tela de certificado (US-18).
 */
@Component
public class OpenPdfCertificateGenerator implements CertificatePdfGeneratorPort {

    private static final Locale PT_BR = Locale.of("pt", "BR");
    private static final DateTimeFormatter CONCLUSION_DATE_FORMAT =
            DateTimeFormatter.ofPattern("dd 'de' MMMM 'de' yyyy", PT_BR);

    private static final float MARGIN = 56f;
    private static final float TITLE_FONT_SIZE = 22f;
    private static final float HIGHLIGHT_FONT_SIZE = 20f;
    private static final float BODY_FONT_SIZE = 12f;
    private static final float SPACING_UNIT = 18f;
    private static final int MINUTES_PER_HOUR = 60;

    @Override
    public byte[] generate(CertificateRecord certificate) {
        final Document document = new Document(PageSize.A4.rotate(), MARGIN, MARGIN, MARGIN, MARGIN);
        final ByteArrayOutputStream output = new ByteArrayOutputStream();
        try {
            PdfWriter.getInstance(document, output);
            document.open();
            addContent(document, certificate);
        } catch (DocumentException e) {
            throw new IllegalStateException("Failed to generate certificate PDF", e);
        } finally {
            document.close();
        }
        return output.toByteArray();
    }

    private void addContent(Document document, CertificateRecord certificate) throws DocumentException {
        final Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, TITLE_FONT_SIZE);
        final Font highlightFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, HIGHLIGHT_FONT_SIZE);
        final Font bodyFont = FontFactory.getFont(FontFactory.HELVETICA, BODY_FONT_SIZE);

        document.add(centered("Certificado de Conclusão", titleFont, 0));
        document.add(centered("Certificamos que", bodyFont, SPACING_UNIT * 2));
        document.add(centered(certificate.studentName(), highlightFont, SPACING_UNIT));
        document.add(centered("concluiu com sucesso o curso", bodyFont, SPACING_UNIT));
        document.add(centered(certificate.courseTitle(), highlightFont, SPACING_UNIT));
        document.add(centered(
                "oferecido pela Escapa! Cursos, com carga horária total de "
                        + formatWorkload(certificate.workloadMinutes()) + ".",
                bodyFont, SPACING_UNIT));
        document.add(centered(
                "Data de conclusão: " + certificate.conclusionDate().format(CONCLUSION_DATE_FORMAT),
                bodyFont, SPACING_UNIT * 2));
        document.add(centered("Código de verificação: " + certificate.verificationCode(), bodyFont, SPACING_UNIT));
    }

    private Paragraph centered(String text, Font font, float spacingBefore) {
        final Paragraph paragraph = new Paragraph(text, font);
        paragraph.setAlignment(Element.ALIGN_CENTER);
        paragraph.setSpacingBefore(spacingBefore);
        return paragraph;
    }

    /** "16 horas" quando exata, "16h 30min" quando fracionada; sem carga horaria cadastrada, um aviso. */
    private String formatWorkload(Integer workloadMinutes) {
        if (workloadMinutes == null || workloadMinutes <= 0) {
            return "carga horária não informada";
        }
        final int hours = workloadMinutes / MINUTES_PER_HOUR;
        final int minutes = workloadMinutes % MINUTES_PER_HOUR;
        if (minutes == 0) {
            return hours + (hours == 1 ? " hora" : " horas");
        }
        return hours + "h " + minutes + "min";
    }
}

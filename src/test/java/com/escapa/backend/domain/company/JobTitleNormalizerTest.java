package com.escapa.backend.domain.company;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.Test;

class JobTitleNormalizerTest {

    @Test
    void shouldBuildTheSameKeyIgnoringCaseAccentsAndExtraSpaces() {
        assertEquals("RECEPCAO", JobTitleNormalizer.key("Recepção"));
        assertEquals("RECEPCAO", JobTitleNormalizer.key("  RECEPCAO  "));
        assertEquals("RECEPCAO", JobTitleNormalizer.key("recepcao"));
        assertEquals("GOVERNANCA HOTELEIRA", JobTitleNormalizer.key("Governança   Hoteleira"));
    }

    @Test
    void shouldFormatDisplayAsTitleCasePreservingAccents() {
        assertEquals("Recepção", JobTitleNormalizer.display("  recepção  "));
        assertEquals("Recepcao", JobTitleNormalizer.display("RECEPCAO"));
        assertEquals("Governança Hoteleira", JobTitleNormalizer.display("governança   hoteleira"));
    }

    @Test
    void shouldReuseAnExistingCompanyLabelWithTheSameKey() {
        assertEquals("Recepção",
                JobTitleNormalizer.resolveForCompany("RECEPCAO", List.of("COLABORADOR", "Recepção")));
        assertEquals("Recepção",
                JobTitleNormalizer.resolveForCompany("recepcao", List.of("Recepção")));
        assertEquals("Cozinha",
                JobTitleNormalizer.resolveForCompany("  cozinha ", List.of("Recepção")));
    }

    @Test
    void shouldPreferAccentedOrTitleCaseLabelsWhenChoosing() {
        assertEquals("Recepção", JobTitleNormalizer.preferLabel("RECEPCAO", "Recepção"));
        assertEquals("Cozinha", JobTitleNormalizer.preferLabel("COZINHA", "Cozinha"));
        assertEquals("Bar", JobTitleNormalizer.preferLabel("Bar", "Cozinha"));
        assertEquals("Bar", JobTitleNormalizer.preferLabel("Bar", "Baz"));
    }

    @Test
    void shouldHandleNullAndBlankInputs() {
        assertEquals("", JobTitleNormalizer.key(null));
        assertEquals("", JobTitleNormalizer.display(null));
        assertEquals("", JobTitleNormalizer.display("   "));
        assertEquals("", JobTitleNormalizer.resolveForCompany("   ", List.of("Recepção")));
    }
}

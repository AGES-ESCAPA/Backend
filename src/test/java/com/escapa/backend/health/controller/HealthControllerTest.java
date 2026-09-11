package com.escapa.backend.health.controller;

import com.escapa.backend.common.WebIntegrationTest;
import org.junit.jupiter.api.Test;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Além do health em si, este teste prova que o contexto completo sobe com a
 * nova estrutura de pacotes e que o handler global está registrado.
 */
class HealthControllerTest extends WebIntegrationTest {

    @Test
    void shouldReportUpInsideApiResponseEnvelope() throws Exception {
        mockMvc.perform(get("/api/v1/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("UP"))
                .andExpect(jsonPath("$.data.service").value("escapa-backend"))
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void unknownRouteShouldReturn404WithErrorEnvelope() throws Exception {
        mockMvc.perform(get("/api/v1/does-not-exist"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"))
                .andExpect(jsonPath("$.path").value("/api/v1/does-not-exist"));
    }
}

package com.escapa.backend.infrastructure.config;

import com.escapa.backend.infrastructure.persistence.PostgresIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.hasKey;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** O Swagger só envia o token nas operações marcadas com bearerAuth; sem a marca, a chamada volta 401. */
@AutoConfigureMockMvc
@ActiveProfiles("test")
class OpenApiSecurityTest extends PostgresIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldAskForTheBearerTokenWhenPlacingAnOrder() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paths['/api/v1/orders'].post.security[0]", hasKey("bearerAuth")))
                .andExpect(jsonPath("$.paths['/api/v1/company/employees'].get.security[0]", hasKey("bearerAuth")));
    }
}

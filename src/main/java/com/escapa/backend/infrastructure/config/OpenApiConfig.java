package com.escapa.backend.infrastructure.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class OpenApiConfig implements WebMvcConfigurer {

    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        registry.addRedirectViewController("/", "/swagger-ui/index.html");
    }

    @Bean
    public OpenAPI escapaOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Escapa! API")
                        .description("API da plataforma de cursos e qualificação profissional em Turismo e Hospitalidade")
                        .version("v1"));
    }
}


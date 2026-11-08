package com.escapa.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.TimeZone;

@SpringBootApplication
public class EscapaBackendApplication {

    /**
     * Fuso unico do sistema. Toda data/hora do projeto (LocalDateTime.now(), LocalDate.now(),
     * Clock.systemDefaultZone()) usa o fuso padrao da JVM, e o driver do PostgreSQL repassa esse
     * mesmo fuso a sessao do banco (CURRENT_DATE, now()). Sem isto, o container (UTC) gravaria
     * as 22h de Brasilia como 01h do dia seguinte.
     */
    public static final String TIME_ZONE = "America/Sao_Paulo";

    static {
        TimeZone.setDefault(TimeZone.getTimeZone(TIME_ZONE));
    }

    public static void main(String[] args) {
        SpringApplication.run(EscapaBackendApplication.class, args);
    }
}

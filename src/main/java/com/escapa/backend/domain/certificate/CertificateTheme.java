package com.escapa.backend.domain.certificate;

import java.util.Locale;

/** Tema visual do PDF do certificado: os mesmos tokens da tela, em versao clara ou escura. */
public enum CertificateTheme {
    LIGHT,
    DARK;

    /**
     * Converte o parametro {@code ?theme=} da URL, sem diferenciar maiusculas.
     *
     * @throws IllegalArgumentException se o valor nao for {@code light} nem {@code dark} (vira 400)
     */
    public static CertificateTheme fromParam(String value) {
        if (value != null) {
            for (CertificateTheme theme : values()) {
                if (theme.name().equalsIgnoreCase(value.trim())) {
                    return theme;
                }
            }
        }
        throw new IllegalArgumentException("Invalid theme '" + value + "': use light or dark");
    }

    /** Valor usado no parametro da URL e no atributo data-theme do template. */
    public String param() {
        return name().toLowerCase(Locale.ROOT);
    }
}

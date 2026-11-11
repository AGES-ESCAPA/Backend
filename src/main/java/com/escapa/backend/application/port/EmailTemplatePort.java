package com.escapa.backend.application.port;

import java.util.Map;

/** Conteúdo dos e-mails, guardado em arquivos de template. Implementado na infraestrutura. */
public interface EmailTemplatePort {

    /**
     * Devolve o template com cada marcador {@code {{chave}}} trocado pelo valor correspondente.
     *
     * @param templateName nome do arquivo, com a extensão (ex.: "convite-colaborador.html")
     */
    String render(String templateName, Map<String, String> variables);
}

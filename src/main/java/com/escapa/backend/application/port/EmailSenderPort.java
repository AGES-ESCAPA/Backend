package com.escapa.backend.application.port;

/**
 * Envio de e-mail. O caso de uso monta a mensagem completa e não sabe se o envio é de
 * teste: a marcação de teste é aplicada pela implementação, conforme o ambiente.
 */
public interface EmailSenderPort {

    /**
     * @param to       endereço do destinatário
     * @param htmlBody corpo em HTML
     * @param textBody o mesmo conteúdo em texto puro, para clientes que não exibem HTML
     */
    void send(String to, String subject, String htmlBody, String textBody);
}

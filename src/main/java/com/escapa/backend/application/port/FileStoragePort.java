package com.escapa.backend.application.port;

import java.io.InputStream;

public interface FileStoragePort {

    /**
     * Envia o arquivo para o storage e retorna a URL publica para acessa-lo.
     *
     * @param key         caminho/identificador do arquivo dentro do bucket (ex.: "avatars/uuid.jpg")
     * @param content     conteudo do arquivo
     * @param size        tamanho em bytes, exigido pelo cliente do storage
     * @param contentType tipo MIME do arquivo (ex.: "image/png")
     */
    String upload(String key, InputStream content, long size, String contentType);

    /**
     * Remove um arquivo do storage a partir da URL publica retornada por {@link #upload}.
     */
    void delete(String url);
}
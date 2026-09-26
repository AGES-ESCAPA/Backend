package com.escapa.backend.application.model;

/** PDF pronto para download: os bytes do arquivo e o nome sugerido para salvar. */
public record CertificateFile(byte[] content, String filename) {
}

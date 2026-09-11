package com.escapa.backend.common.api;

import java.time.Instant;

/**
 * Envelope padrão de toda resposta de erro.
 *
 * @param status    código HTTP numérico
 * @param error     frase do status HTTP (ex.: "Not Found")
 * @param code      código estável legível por máquina (ex.: "USER_NOT_FOUND")
 * @param message   descrição para humanos
 * @param path      URI da requisição
 * @param timestamp instante em que o erro foi gerado
 */
public record ApiError(int status, String error, String code, String message, String path, Instant timestamp) {
}

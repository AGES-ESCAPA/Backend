package com.escapa.backend.common.exception;

/**
 * Base de toda exceção de regra de negócio.
 *
 * <p>Carrega um {@code code} estável, legível por máquina (ex.: {@code USER_NOT_FOUND}),
 * que o frontend usa para decidir o que exibir. A mensagem é para humanos.
 *
 * <p>As features nunca escolhem status HTTP: elas herdam de uma das três filhas
 * ({@link NotFoundException}, {@link ConflictException}, {@link BusinessRuleException})
 * e o {@code GlobalExceptionHandler} em {@code common.api} decide o status.
 */
public abstract class BusinessException extends RuntimeException {

    private final String code;

    protected BusinessException(String code, String message) {
        super(message);
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}

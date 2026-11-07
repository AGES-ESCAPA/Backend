package com.escapa.backend.application.usecase;

/**
 * Regra de paginação dos casos de uso que recusam o pedido inválido (em vez de ajustá-lo): página a
 * partir de zero e tamanho de 1 a {@value #MAX_PAGE_SIZE}. Vira {@code 400} na camada web.
 */
final class PaginationRules {

    static final int MAX_PAGE_SIZE = 100;

    private PaginationRules() {
    }

    static void requireValid(int page, int size) {
        if (page < 0 || size < 1 || size > MAX_PAGE_SIZE) {
            throw new IllegalArgumentException("Invalid pagination");
        }
    }
}

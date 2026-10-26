package com.escapa.backend.adapters.controller;

/** Valores padrão (como texto, exigido por {@code @RequestParam.defaultValue}) dos parâmetros de paginação. */
final class PaginationDefaults {

    static final String FIRST_PAGE = "0";
    static final String PAGE_SIZE = "10";
    static final String CHANGE_LOG_PAGE_SIZE = "20";

    private PaginationDefaults() {
    }
}

package com.escapa.backend.adapters.dto;

/** Limites de tamanho dos campos de texto, alinhados às colunas VARCHAR(255) do schema. */
public final class FieldLimits {

    public static final int VARCHAR_MAX = 255;
    public static final String VARCHAR_MESSAGE = "must have at most 255 characters";

    private FieldLimits() {
    }
}

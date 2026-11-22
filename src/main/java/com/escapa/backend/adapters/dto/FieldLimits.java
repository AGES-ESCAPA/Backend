package com.escapa.backend.adapters.dto;

/** Limites de tamanho dos campos de texto, alinhados às colunas do schema. */
public final class FieldLimits {

    public static final int VARCHAR_MAX = 255;
    public static final String VARCHAR_MESSAGE = "must have at most 255 characters";

    /** Bio do instrutor (coluna TEXT em admins). */
    public static final int BIO_MAX = 4000;
    public static final String BIO_MESSAGE = "must have at most 4000 characters";

    private FieldLimits() {
    }
}

package com.escapa.backend.domain.order;

/** Quem compra já tem acesso ativo ao curso, então comprar de novo seria cobrar duas vezes pelo mesmo acesso. */
public class CourseAlreadyActiveException extends RuntimeException {

    public CourseAlreadyActiveException(String message) {
        super(message);
    }
}

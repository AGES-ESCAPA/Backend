package com.escapa.backend.domain.company;

/** A empresa já usa todas as vagas contratadas: cadastrar outro colaborador exige comprar mais vagas. */
public class NoSeatsAvailableException extends RuntimeException {

    public NoSeatsAvailableException(int seatsTotal) {
        super("No free seat: all " + seatsTotal + " seat(s) of the company are in use");
    }
}

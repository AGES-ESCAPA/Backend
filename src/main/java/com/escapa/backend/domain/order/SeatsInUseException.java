package com.escapa.backend.domain.order;

/**
 * Cancelar o pedido deixaria a empresa com menos assentos do que os colaboradores
 * que já os ocupam. Informa os números para o gestor saber quantos liberar.
 */
public class SeatsInUseException extends RuntimeException {

    private final int seatsInUse;
    private final int seatsAfterCancellation;

    public SeatsInUseException(int seatsInUse, int seatsAfterCancellation) {
        super("Cannot cancel the order: " + seatsInUse + " seat(s) in use but only "
                + seatsAfterCancellation + " would remain; release "
                + (seatsInUse - seatsAfterCancellation) + " seat(s) first");
        this.seatsInUse = seatsInUse;
        this.seatsAfterCancellation = seatsAfterCancellation;
    }

    public int getSeatsInUse() {
        return seatsInUse;
    }

    public int getSeatsAfterCancellation() {
        return seatsAfterCancellation;
    }

    public int getSeatsToRelease() {
        return seatsInUse - seatsAfterCancellation;
    }
}

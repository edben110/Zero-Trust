package com.zerotrust.votingapi.exception;

public final class VotoAnuladoException extends RuntimeException {

    public VotoAnuladoException() {
        super("el voto fue anulado por dos errores de región", null, false, false);
    }
}

package com.zerotrust.votingapi.domain;

public final class VotoAnuladoException extends RuntimeException {

    public VotoAnuladoException() {
        super("el voto fue anulado por dos errores de región", null, false, false);
    }
}

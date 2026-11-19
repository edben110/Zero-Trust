package com.zerotrust.votingapi.exception;

public class VotanteNoHabilitadoException extends RuntimeException {

    public VotanteNoHabilitadoException() {
        super("el votante no está habilitado", null, false, false);
    }
}

package com.zerotrust.votingapi.domain;

public class VotoDuplicadoException extends RuntimeException {

    public VotoDuplicadoException() {
        super("ya existe un voto para este votante en esta elección", null, false, false);
    }
}

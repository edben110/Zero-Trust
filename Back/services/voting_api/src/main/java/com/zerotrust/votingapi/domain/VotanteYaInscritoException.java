package com.zerotrust.votingapi.domain;

public class VotanteYaInscritoException extends RuntimeException {

    public VotanteYaInscritoException() {
        super("el documento ya está inscrito en el padrón", null, false, false);
    }
}

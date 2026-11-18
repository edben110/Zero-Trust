package com.zerotrust.votingapi.domain;

public class VotanteFueraDelPadronException extends RuntimeException {

    public VotanteFueraDelPadronException() {
        super("el titular del documento no está en el padrón", null, false, false);
    }
}

package com.zerotrust.votingapi.exception;

public final class RegionIncorrectaException extends RuntimeException {

    public RegionIncorrectaException() {
        super("la región elegida no coincide con la registrada", null, false, false);
    }
}

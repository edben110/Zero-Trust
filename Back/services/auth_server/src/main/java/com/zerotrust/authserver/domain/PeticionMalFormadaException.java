package com.zerotrust.authserver.domain;

public class PeticionMalFormadaException extends ErrorOAuthException {

    public PeticionMalFormadaException() {
        this(null);
    }

    public PeticionMalFormadaException(String clientIdCrudo) {
        super(CodigoErrorOAuth.INVALID_REQUEST, clientIdCrudo);
    }
}

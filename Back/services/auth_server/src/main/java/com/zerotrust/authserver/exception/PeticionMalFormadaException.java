package com.zerotrust.authserver.exception;

import com.zerotrust.authserver.domain.CodigoErrorOAuth;

public class PeticionMalFormadaException extends ErrorOAuthException {

    public PeticionMalFormadaException() {
        this(null);
    }

    public PeticionMalFormadaException(String clientIdCrudo) {
        super(CodigoErrorOAuth.INVALID_REQUEST, clientIdCrudo);
    }
}

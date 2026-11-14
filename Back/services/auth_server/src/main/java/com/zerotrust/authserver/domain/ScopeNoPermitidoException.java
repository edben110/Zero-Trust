package com.zerotrust.authserver.domain;

public class ScopeNoPermitidoException extends ErrorOAuthException {

    public ScopeNoPermitidoException() {
        this(null);
    }

    public ScopeNoPermitidoException(String clientIdCrudo) {
        super(CodigoErrorOAuth.INVALID_SCOPE, clientIdCrudo);
    }
}

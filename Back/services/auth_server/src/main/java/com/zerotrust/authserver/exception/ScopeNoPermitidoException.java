package com.zerotrust.authserver.exception;

import com.zerotrust.authserver.domain.CodigoErrorOAuth;

public class ScopeNoPermitidoException extends ErrorOAuthException {

    public ScopeNoPermitidoException() {
        this(null);
    }

    public ScopeNoPermitidoException(String clientIdCrudo) {
        super(CodigoErrorOAuth.INVALID_SCOPE, clientIdCrudo);
    }
}

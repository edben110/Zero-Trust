package com.zerotrust.authserver.exception;

import com.zerotrust.authserver.domain.CodigoErrorOAuth;

public class GrantNoSoportadoException extends ErrorOAuthException {

    public GrantNoSoportadoException(String clientIdCrudo) {
        super(CodigoErrorOAuth.UNSUPPORTED_GRANT_TYPE, clientIdCrudo);
    }
}

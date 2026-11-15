package com.zerotrust.authserver.domain;

public class GrantNoSoportadoException extends ErrorOAuthException {

    public GrantNoSoportadoException(String clientIdCrudo) {
        super(CodigoErrorOAuth.UNSUPPORTED_GRANT_TYPE, clientIdCrudo);
    }
}

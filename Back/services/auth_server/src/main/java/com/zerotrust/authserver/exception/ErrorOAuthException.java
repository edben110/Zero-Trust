package com.zerotrust.authserver.exception;

import com.zerotrust.authserver.domain.CodigoErrorOAuth;

import java.util.Optional;

public abstract class ErrorOAuthException extends RuntimeException {

    private final CodigoErrorOAuth codigo;
    private final transient String clientIdCrudo;

    protected ErrorOAuthException(CodigoErrorOAuth codigo, String clientIdCrudo) {
        super(codigo.texto(), null, false, false);
        this.codigo = codigo;
        this.clientIdCrudo = clientIdCrudo;
    }

    public CodigoErrorOAuth codigo() {
        return codigo;
    }

    // Solo sirve para comprobar si el cliente está registrado; nunca se escribe en el log tal cual.
    public Optional<String> clientIdCrudo() {
        return Optional.ofNullable(clientIdCrudo);
    }
}

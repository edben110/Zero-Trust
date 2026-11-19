package com.zerotrust.votingapi.exception;

// El mensaje se muestra tal cual al administrador.
public class SolicitudSeguridadInvalidaException extends IllegalArgumentException {

    public SolicitudSeguridadInvalidaException(String mensaje) {
        super(mensaje);
    }
}

package com.zerotrust.votingapi.exception;

// El mensaje se muestra tal cual al administrador.
public class OperacionSeguridadNoDisponibleException extends RuntimeException {

    public OperacionSeguridadNoDisponibleException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}

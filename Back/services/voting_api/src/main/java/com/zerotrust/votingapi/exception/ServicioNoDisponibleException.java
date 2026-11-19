package com.zerotrust.votingapi.exception;

public abstract class ServicioNoDisponibleException extends RuntimeException {

    protected ServicioNoDisponibleException(String mensaje) {
        super(mensaje, null, false, false);
    }
}

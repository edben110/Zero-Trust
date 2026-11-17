package com.zerotrust.votingapi.domain;

public abstract class ServicioNoDisponibleException extends RuntimeException {

    protected ServicioNoDisponibleException(String mensaje) {
        super(mensaje, null, false, false);
    }
}

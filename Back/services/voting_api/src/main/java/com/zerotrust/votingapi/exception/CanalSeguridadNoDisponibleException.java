package com.zerotrust.votingapi.exception;

public class CanalSeguridadNoDisponibleException extends RuntimeException {

    public CanalSeguridadNoDisponibleException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}

package com.zerotrust.votingapi.exception;

public class ValidacionNoDisponibleException extends ServicioNoDisponibleException {

    public ValidacionNoDisponibleException() {
        super("la validación del votante no está disponible");
    }
}

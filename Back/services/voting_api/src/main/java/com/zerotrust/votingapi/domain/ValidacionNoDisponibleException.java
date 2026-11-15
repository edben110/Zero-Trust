package com.zerotrust.votingapi.domain;

public class ValidacionNoDisponibleException extends ServicioNoDisponibleException {

    public ValidacionNoDisponibleException() {
        super("la validación del votante no está disponible");
    }
}

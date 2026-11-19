package com.zerotrust.votingapi.exception;

public class PublicacionNoDisponibleException extends ServicioNoDisponibleException {

    public PublicacionNoDisponibleException() {
        super("no se pudo publicar el voto");
    }
}

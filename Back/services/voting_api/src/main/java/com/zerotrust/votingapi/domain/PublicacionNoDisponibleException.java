package com.zerotrust.votingapi.domain;

public class PublicacionNoDisponibleException extends ServicioNoDisponibleException {

    public PublicacionNoDisponibleException() {
        super("no se pudo publicar el voto");
    }
}

package com.zerotrust.votingapi.domain;

import java.util.List;

public class SolicitudVotoInvalidaException extends RuntimeException {

    private final transient List<String> campos;

    public SolicitudVotoInvalidaException(List<String> campos) {
        super("solicitud de voto inválida", null, false, false);
        this.campos = List.copyOf(campos);
    }

    public List<String> campos() {
        return campos;
    }
}

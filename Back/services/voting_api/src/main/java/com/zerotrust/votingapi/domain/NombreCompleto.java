package com.zerotrust.votingapi.domain;

// Nombre del votante tal como se inscribe en el padrón (registered_voters.full_name, VARCHAR(255)).
public record NombreCompleto(String valor) {

    private static final int LONGITUD_MAXIMA = 255;

    public NombreCompleto {
        if (valor == null || valor.isBlank()) {
            throw new IllegalArgumentException("fullName no puede estar vacío");
        }
        valor = valor.strip();
        if (valor.length() > LONGITUD_MAXIMA) {
            throw new IllegalArgumentException("fullName admite como máximo " + LONGITUD_MAXIMA + " caracteres");
        }
    }
}

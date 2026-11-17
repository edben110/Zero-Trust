package com.zerotrust.voterregistration.domain;

public record NombreCompleto(String valor) {

    // registered_voters.full_name es VARCHAR(255).
    private static final int LARGO_MAXIMO = 255;

    public NombreCompleto {
        if (valor == null || valor.isBlank() || valor.length() > LARGO_MAXIMO) {
            throw new IllegalArgumentException("fullName debe tener entre 1 y 255 caracteres");
        }
    }

    // Dato personal: no debe aparecer en logs ni en mensajes de error.
    @Override
    public String toString() {
        return "NombreCompleto[****]";
    }
}

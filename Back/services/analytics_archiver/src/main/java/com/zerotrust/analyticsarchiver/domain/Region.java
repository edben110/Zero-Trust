package com.zerotrust.analyticsarchiver.domain;

public record Region(String valor) {

    // vote_audit.region es VARCHAR(100).
    private static final int LARGO_MAXIMO = 100;

    public Region {
        if (valor == null || valor.isBlank() || valor.length() > LARGO_MAXIMO) {
            throw new IllegalArgumentException("region debe tener entre 1 y 100 caracteres");
        }
    }
}

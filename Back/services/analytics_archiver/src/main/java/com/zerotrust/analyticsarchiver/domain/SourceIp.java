package com.zerotrust.analyticsarchiver.domain;

public record SourceIp(String valor) {

    // vote_audit.source_ip es VARCHAR(45), el largo máximo de una IPv6 en texto.
    private static final int LARGO_MAXIMO = 45;

    public SourceIp {
        if (valor == null || valor.isBlank() || valor.length() > LARGO_MAXIMO) {
            throw new IllegalArgumentException("sourceIp debe tener entre 1 y 45 caracteres");
        }
    }
}

package com.zerotrust.votingapi.domain;

public record Region(String valor) {

    public Region {
        if (valor == null || valor.isBlank()) {
            throw new IllegalArgumentException("region no puede estar vacía");
        }
    }
}

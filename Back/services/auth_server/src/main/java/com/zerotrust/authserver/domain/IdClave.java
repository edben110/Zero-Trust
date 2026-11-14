package com.zerotrust.authserver.domain;

public record IdClave(String valor) {

    public IdClave {
        if (valor == null || valor.isBlank()) {
            throw new IllegalArgumentException("AUTH_SIGNING_KEY_ID no puede estar vacío");
        }
    }
}

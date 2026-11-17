package com.zerotrust.authserver.domain;

import java.util.Arrays;

public enum Audiencia {
    KAFKA("kafka"),
    RABBITMQ("rabbitmq");

    private final String valor;

    Audiencia(String valor) {
        this.valor = valor;
    }

    public String valor() {
        return valor;
    }

    public static Audiencia desde(String texto) {
        return Arrays.stream(values())
                .filter(audiencia -> audiencia.valor.equals(texto))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("audiencia desconocida"));
    }
}

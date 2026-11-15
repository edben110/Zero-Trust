package com.zerotrust.voterregistration.domain;

import java.util.Locale;
import java.util.regex.Pattern;

public record VoterId(String valor) {

    private static final Pattern UUID_CANONICO =
            Pattern.compile("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}");

    public VoterId {
        if (valor == null) {
            throw new IllegalArgumentException("voterId no puede ser nulo");
        }
        valor = valor.toLowerCase(Locale.ROOT);
        if (!UUID_CANONICO.matcher(valor).matches()) {
            throw new IllegalArgumentException("voterId debe ser un UUID canónico");
        }
    }
}

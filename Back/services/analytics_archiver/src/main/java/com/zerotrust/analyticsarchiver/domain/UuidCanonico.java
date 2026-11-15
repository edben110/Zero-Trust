package com.zerotrust.analyticsarchiver.domain;

import java.util.Locale;
import java.util.regex.Pattern;

final class UuidCanonico {

    private static final Pattern FORMATO =
            Pattern.compile("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}");

    private UuidCanonico() {
    }

    static String normalizar(String valor, String nombre) {
        if (valor == null) {
            throw new IllegalArgumentException(nombre + " no puede ser nulo");
        }
        String minusculas = valor.toLowerCase(Locale.ROOT);
        if (!FORMATO.matcher(minusculas).matches()) {
            throw new IllegalArgumentException(nombre + " debe ser un UUID canónico");
        }
        return minusculas;
    }
}

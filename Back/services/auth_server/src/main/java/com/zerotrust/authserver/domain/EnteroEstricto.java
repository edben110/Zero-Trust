package com.zerotrust.authserver.domain;

import java.util.regex.Pattern;

final class EnteroEstricto {

    private static final Pattern DIGITOS = Pattern.compile("\\d{1,9}");

    private EnteroEstricto() {
    }

    static int desde(String texto, String nombre) {
        if (texto == null || !DIGITOS.matcher(texto).matches()) {
            throw new IllegalArgumentException(nombre + " debe ser un entero positivo");
        }
        return Integer.parseInt(texto);
    }
}

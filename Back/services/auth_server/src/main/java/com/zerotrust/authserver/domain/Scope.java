package com.zerotrust.authserver.domain;

import java.util.regex.Pattern;

public record Scope(String valor) {

    // scope-token de RFC 6749 §3.3: uno o más caracteres %x21 / %x23-5B / %x5D-7E.
    private static final Pattern SCOPE_TOKEN = Pattern.compile("[\\x21\\x23-\\x5B\\x5D-\\x7E]+");

    public Scope {
        if (valor == null || !SCOPE_TOKEN.matcher(valor).matches()) {
            throw new IllegalArgumentException("el scope no cumple la gramática scope-token");
        }
    }
}

package com.zerotrust.voterregistration.domain;

import java.util.regex.Pattern;

// Lo fija el servidor (ELECTION_ID), nunca el cliente.
public record ElectionId(String valor) {

    private static final Pattern FORMATO = Pattern.compile("[A-Za-z0-9][A-Za-z0-9._-]{0,63}");

    public ElectionId {
        if (valor == null || !FORMATO.matcher(valor).matches()) {
            throw new IllegalArgumentException("electionId debe tener de 1 a 64 caracteres: letras, dígitos, '.', '_' o '-'");
        }
    }
}

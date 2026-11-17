package com.zerotrust.authserver.domain;

import java.nio.charset.StandardCharsets;

public record SecretoCliente(String valor) {

    private static final int LIMITE_BYTES_BCRYPT = 72;

    public SecretoCliente {
        if (valor == null || valor.isEmpty()) {
            throw new IllegalArgumentException("client_secret no puede ser nulo ni vacío");
        }
    }

    // BCrypt ignora lo que pasa de 72 bytes: dos secretos con el mismo prefijo serían equivalentes.
    public boolean excedeLimiteBcrypt() {
        return valor.getBytes(StandardCharsets.UTF_8).length > LIMITE_BYTES_BCRYPT;
    }

    @Override
    public String toString() {
        return "SecretoCliente[****]";
    }
}

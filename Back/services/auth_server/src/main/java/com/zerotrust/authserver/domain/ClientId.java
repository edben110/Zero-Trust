package com.zerotrust.authserver.domain;

import java.util.Optional;

public record ClientId(String valor) {

    public ClientId {
        if (valor == null || valor.isEmpty()) {
            throw new IllegalArgumentException("client_id no puede ser nulo ni vacío");
        }
        if (Espacios.contieneEspacioEnBlanco(valor)) {
            throw new IllegalArgumentException("client_id no puede contener espacios en blanco");
        }
    }

    public static Optional<ClientId> intentar(String crudo) {
        if (crudo == null || crudo.isEmpty() || Espacios.contieneEspacioEnBlanco(crudo)) {
            return Optional.empty();
        }
        return Optional.of(new ClientId(crudo));
    }
}

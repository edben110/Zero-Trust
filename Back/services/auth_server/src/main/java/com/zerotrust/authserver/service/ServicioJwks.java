package com.zerotrust.authserver.service;

import com.zerotrust.authserver.port.PublicadorClaves;

import java.util.Map;

public class ServicioJwks {

    private final PublicadorClaves publicador;

    public ServicioJwks(PublicadorClaves publicador) {
        this.publicador = publicador;
    }

    public Map<String, Object> jwkSet() {
        return publicador.jwkSetPublico();
    }
}

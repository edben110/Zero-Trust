package com.zerotrust.votingapi.domain;

public record UmbralesSeguridad(int sospechosa, int bloqueada, int ventanaSegundos) {

    public UmbralesSeguridad {
        if (sospechosa <= 0 || bloqueada <= sospechosa) {
            throw new IllegalArgumentException("Los umbrales de seguridad no son coherentes");
        }
        if (ventanaSegundos <= 0) {
            throw new IllegalArgumentException("La ventana de seguridad debe ser positiva");
        }
    }
}

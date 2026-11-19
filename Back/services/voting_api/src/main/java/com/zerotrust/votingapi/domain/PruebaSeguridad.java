package com.zerotrust.votingapi.domain;

import com.zerotrust.votingapi.exception.SolicitudSeguridadInvalidaException;

public record PruebaSeguridad(DireccionIp ip, int votos) {

    public static final int MAX_VOTOS = 50;

    public PruebaSeguridad {
        if (ip == null) {
            throw new SolicitudSeguridadInvalidaException("La IP es obligatoria.");
        }
        if (votos < 1 || votos > MAX_VOTOS) {
            throw new SolicitudSeguridadInvalidaException(
                    "El número de votos debe estar entre 1 y " + MAX_VOTOS + ".");
        }
    }

    public static PruebaSeguridad de(String ip, Integer votos) {
        return new PruebaSeguridad(new DireccionIp(ip), votos == null ? 1 : votos);
    }
}

package com.zerotrust.votingapi.domain;

/**
 * Datos inválidos en una operación del panel de seguridad. El mensaje se muestra tal cual al administrador.
 */
public class SolicitudSeguridadInvalidaException extends IllegalArgumentException {

    public SolicitudSeguridadInvalidaException(String mensaje) {
        super(mensaje);
    }
}

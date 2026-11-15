package com.zerotrust.authserver.domain;

// Nunca encadena la causa original: su mensaje es lo único que llega al log de arranque.
public class ConfiguracionInvalidaException extends RuntimeException {

    public ConfiguracionInvalidaException(String mensaje) {
        super(mensaje, null, false, false);
    }
}

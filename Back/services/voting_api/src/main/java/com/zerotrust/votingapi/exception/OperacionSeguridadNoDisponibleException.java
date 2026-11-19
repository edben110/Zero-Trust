package com.zerotrust.votingapi.exception;

/**
 * Una operación del panel de seguridad no se completó por Kafka o PostgreSQL.
 * El mensaje se muestra tal cual al administrador.
 */
public class OperacionSeguridadNoDisponibleException extends RuntimeException {

    public OperacionSeguridadNoDisponibleException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}

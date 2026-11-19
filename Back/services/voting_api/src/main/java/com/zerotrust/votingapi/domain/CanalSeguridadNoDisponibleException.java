package com.zerotrust.votingapi.domain;

/**
 * Un adaptador del panel de seguridad (Kafka o PostgreSQL) no respondió.
 */
public class CanalSeguridadNoDisponibleException extends RuntimeException {

    public CanalSeguridadNoDisponibleException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}

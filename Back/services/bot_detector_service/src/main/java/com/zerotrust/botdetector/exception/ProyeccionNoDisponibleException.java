package com.zerotrust.botdetector.exception;

/**
 * No se pudo actualizar la proyección del estado de una IP (ip_risk_state).
 */
public class ProyeccionNoDisponibleException extends RuntimeException {

    public ProyeccionNoDisponibleException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}

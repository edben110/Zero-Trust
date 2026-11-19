package com.zerotrust.botdetector.domain.exception;

/**
 * No se pudo actualizar la proyección del estado de una IP (ip_risk_state).
 */
public class ProyeccionNoDisponible extends RuntimeException {

    public ProyeccionNoDisponible(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}

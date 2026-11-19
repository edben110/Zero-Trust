package com.zerotrust.botdetector.domain;

/**
 * Origen de una alerta de seguridad.
 *
 * LIVE: voto real consumido de raw_votes.
 * DRILL: prueba controlada lanzada desde el panel de administración.
 *
 * Separar el origen permite auditar sin confundir un ataque real con una
 * simulación de laboratorio.
 */
public enum AlertOrigin {
    LIVE,
    DRILL
}

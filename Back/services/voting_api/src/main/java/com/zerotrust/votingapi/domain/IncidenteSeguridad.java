package com.zerotrust.votingapi.domain;

import java.time.Instant;

/**
 * Fila de fraud_incidents tal como la muestra el panel.
 */
public record IncidenteSeguridad(long id, String ip, String tipo, String nivelRiesgo, int votos,
                                 int ventanaSegundos, String origen, Instant detectadoEn) {
}

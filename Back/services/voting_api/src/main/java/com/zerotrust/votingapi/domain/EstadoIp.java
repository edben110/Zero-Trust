package com.zerotrust.votingapi.domain;

import java.time.Instant;

/**
 * Estado vigente de una IP según la proyección ip_risk_state.
 */
public record EstadoIp(String ip, String nivelRiesgo, int votos, int ventanaSegundos,
                       Instant primeraVez, Instant ultimaVez) {
}

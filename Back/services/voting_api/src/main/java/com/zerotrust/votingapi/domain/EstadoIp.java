package com.zerotrust.votingapi.domain;

import java.time.Instant;

public record EstadoIp(String ip, String nivelRiesgo, int votos, int ventanaSegundos,
                       Instant primeraVez, Instant ultimaVez) {
}

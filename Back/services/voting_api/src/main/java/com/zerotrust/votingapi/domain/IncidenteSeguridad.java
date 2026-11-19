package com.zerotrust.votingapi.domain;

import java.time.Instant;

public record IncidenteSeguridad(long id, String ip, String tipo, String nivelRiesgo, int votos,
                                 int ventanaSegundos, String origen, Instant detectadoEn) {
}

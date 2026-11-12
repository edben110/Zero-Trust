package com.zerotrust.botdetector.domain.model;

import java.time.Instant;

/**
 * Contrato de red inmutable para una alerta de seguridad.
 * Payload para el topic security_alerts de Kafka.
 */
public record SecurityAlert(
        String  ip,
        String  riskLevel,
        int     voteCount,
        int     windowSeconds,
        Instant detectedAt
) {
    public SecurityAlert {
        if (ip == null || ip.isBlank()) {
            throw new IllegalArgumentException("ip no puede ser nula ni vacía");
        }
        if (riskLevel == null || riskLevel.isBlank()) {
            throw new IllegalArgumentException("riskLevel no puede ser nulo ni vacío");
        }
        if (voteCount < 0) {
            throw new IllegalArgumentException("voteCount no puede ser negativo");
        }
        if (detectedAt == null) {
            throw new IllegalArgumentException("detectedAt no puede ser nulo");
        }
    }
}

package com.zerotrust.botdetector.domain.model;

import java.time.Instant;

/**
 * Contrato de red inmutable para una alerta de seguridad.
 * Payload para el topic security_alerts de Kafka y registro en PostgreSQL.
 */
public record SecurityAlert(
        String      ip,
        String      riskLevel,
        int         voteCount,
        int         windowSeconds,
        AlertOrigin origin,
        FraudKind   kind,
        Instant     detectedAt
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
        if (windowSeconds <= 0) {
            throw new IllegalArgumentException("windowSeconds debe ser mayor que 0");
        }
        if (origin == null) {
            throw new IllegalArgumentException("origin no puede ser nulo");
        }
        if (kind == null) {
            throw new IllegalArgumentException("kind no puede ser nulo");
        }
        if (detectedAt == null) {
            throw new IllegalArgumentException("detectedAt no puede ser nulo");
        }
    }

    /** Alerta del patrón State: conserva el contrato anterior sin el campo kind. */
    public SecurityAlert(String ip, String riskLevel, int voteCount, int windowSeconds,
                         AlertOrigin origin, Instant detectedAt) {
        this(ip, riskLevel, voteCount, windowSeconds, origin, FraudKind.IP_PATTERN, detectedAt);
    }
}

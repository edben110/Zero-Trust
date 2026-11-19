package com.zerotrust.botdetector.domain;

/**
 * Origen del incidente registrado en fraud_incidents.
 *
 * - IP_PATTERN: el patrón State de la IP superó un umbral (detección de bots).
 * - DUPLICATE_VOTE: voting_api rechazó un segundo voto del mismo documento.
 * - WRONG_REGION: voting_api anuló el voto por dos regiones incorrectas.
 *
 * Los tres tipos comparten el mismo histórico y el mismo topic de alertas para
 * que el panel de administración tenga una sola vista de incidentes.
 */
public enum FraudKind {
    IP_PATTERN,
    DUPLICATE_VOTE,
    WRONG_REGION
}

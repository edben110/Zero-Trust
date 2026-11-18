package com.zerotrust.votingapi.domain;

/**
 * Tipos de incidente de fraude electoral que detecta voting_api.
 *
 * No son patrones de tráfico por IP (eso lo resuelve bot_detector_service con
 * su patrón State), sino hechos del proceso de votación que deben quedar en el
 * histórico de fraud_incidents:
 *
 * - DUPLICATE_VOTE: el documento ya había emitido un voto en esta elección.
 * - WRONG_REGION: el voto fue anulado por acumular dos regiones incorrectas.
 */
public enum FraudKind {
    DUPLICATE_VOTE,
    WRONG_REGION
}

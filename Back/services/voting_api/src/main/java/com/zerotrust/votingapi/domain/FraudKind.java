package com.zerotrust.votingapi.domain;

public enum FraudKind {
    DUPLICATE_VOTE,
    // Voto anulado por acumular dos regiones incorrectas.
    WRONG_REGION
}

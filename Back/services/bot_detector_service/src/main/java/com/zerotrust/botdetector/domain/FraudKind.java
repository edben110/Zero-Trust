package com.zerotrust.botdetector.domain;

public enum FraudKind {
    IP_PATTERN,
    DUPLICATE_VOTE,
    // voting_api anuló el voto por dos regiones incorrectas.
    WRONG_REGION
}

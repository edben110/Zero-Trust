package com.zerotrust.regionalrollup.domain.model;

import java.time.Instant;
import java.util.UUID;

public record RawVoteEvent(
        String voteId,
        String voterId,
        String candidateId,
        String region,
        String sourceIp,
        Instant timestamp
) {
    public RawVoteEvent {
        validateUuid(voteId,      "voteId");
        validateUuid(voterId,     "voterId");
        validateUuid(candidateId, "candidateId");
        if (region == null || region.isBlank()) {
            throw new IllegalArgumentException("region no puede ser nula ni vacía");
        }
        if (sourceIp == null || sourceIp.isBlank()) {
            throw new IllegalArgumentException("sourceIp no puede ser nulo ni vacío");
        }
        if (timestamp == null) {
            throw new IllegalArgumentException("timestamp no puede ser nulo");
        }
    }

    private static void validateUuid(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " no puede ser nulo ni vacío");
        }
        UUID.fromString(value);
    }
}

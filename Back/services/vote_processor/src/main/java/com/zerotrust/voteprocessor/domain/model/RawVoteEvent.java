package com.zerotrust.voteprocessor.domain.model;

import java.time.Instant;

/**
 * Contrato de red inmutable que representa un voto entrante desde el topic raw_votes.
 * Payload JSON: {"voteId":"<uuid>","voterId":"<uuid>","candidateId":"<uuid>",
 *                "region":"<string>","sourceIp":"<string>","timestamp":"<ISO-8601>"}
 */
public record RawVoteEvent(
        VoteId voteId,
        VoterId voterId,
        CandidateId candidateId,
        String region,
        String sourceIp,
        Instant timestamp
) {
    public RawVoteEvent {
        if (region == null || region.isBlank()) {
            throw new IllegalArgumentException("region no puede ser nula ni vacía");
        }
        if (sourceIp == null || sourceIp.isBlank()) {
            throw new IllegalArgumentException("sourceIp no puede ser nula ni vacía");
        }
        if (timestamp == null) {
            throw new IllegalArgumentException("timestamp no puede ser nulo");
        }
    }
}

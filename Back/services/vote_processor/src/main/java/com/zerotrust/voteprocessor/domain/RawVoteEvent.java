package com.zerotrust.voteprocessor.domain;

import java.time.Instant;

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

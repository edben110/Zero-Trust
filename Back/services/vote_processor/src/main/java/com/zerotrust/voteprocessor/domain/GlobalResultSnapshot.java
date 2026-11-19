package com.zerotrust.voteprocessor.domain;

import java.time.Instant;
import java.util.List;

public record GlobalResultSnapshot(Instant timestamp, List<CandidateResult> results) {

    public GlobalResultSnapshot {
        if (timestamp == null) {
            throw new IllegalArgumentException("timestamp no puede ser nulo");
        }
        if (results == null) {
            throw new IllegalArgumentException("results no puede ser nulo");
        }
        results = List.copyOf(results);
    }
}

package com.zerotrust.voteprocessor.domain.model;

import java.time.Instant;
import java.util.List;

/**
 * Snapshot del conteo global en un instante dado.
 * Payload para live_results_global (fanout exchange).
 */
public record GlobalResultSnapshot(Instant timestamp, List<CandidateResult> results) {

    public GlobalResultSnapshot {
        if (timestamp == null) {
            throw new IllegalArgumentException("timestamp no puede ser nulo");
        }
        if (results == null) {
            throw new IllegalArgumentException("results no puede ser nulo");
        }
        // Copia defensiva para garantizar inmutabilidad
        results = List.copyOf(results);
    }
}

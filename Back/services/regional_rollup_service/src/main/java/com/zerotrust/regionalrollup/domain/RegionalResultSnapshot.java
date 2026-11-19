package com.zerotrust.regionalrollup.domain;

import java.time.Instant;
import java.util.List;

public record RegionalResultSnapshot(
        String region,
        Instant timestamp,
        List<RegionalCandidateResult> results
) {
    public RegionalResultSnapshot {
        if (region == null || region.isBlank()) {
            throw new IllegalArgumentException("region no puede ser nula ni vacía");
        }
        if (timestamp == null) {
            throw new IllegalArgumentException("timestamp no puede ser nulo");
        }
        if (results == null) {
            throw new IllegalArgumentException("results no puede ser nulo");
        }
        results = List.copyOf(results);
    }
}

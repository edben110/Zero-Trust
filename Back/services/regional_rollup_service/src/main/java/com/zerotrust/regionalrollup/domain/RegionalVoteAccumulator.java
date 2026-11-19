package com.zerotrust.regionalrollup.domain;

import com.zerotrust.regionalrollup.domain.strategy.RegionalConsolidationStrategy;

import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

// Thread-safe: el consumidor de Kafka y el publicador programado comparten la instancia.
public final class RegionalVoteAccumulator {

    private final RegionalConsolidationStrategy strategy;

    // Mismo criterio que el agregado nacional: identidad global, no por región.
    private final Set<String> seenVoters = new HashSet<>();
    private final Map<String, Map<String, Long>> counts = new HashMap<>();

    public RegionalVoteAccumulator(RegionalConsolidationStrategy strategy) {
        this.strategy = strategy;
    }

    public synchronized boolean accept(RawVoteEvent event) {
        String region     = event.region();
        String voterId    = event.voterId();
        String candidateId = event.candidateId();

        if (seenVoters.contains(voterId)) {
            return false;
        }
        seenVoters.add(voterId);

        Map<String, Long> regionCounts = counts.computeIfAbsent(region, k -> new HashMap<>());
        regionCounts.merge(candidateId, 1L, Long::sum);
        return true;
    }

    public synchronized RegionalResultSnapshot snapshotFor(String region) {
        Map<String, Long> regionCounts = counts.getOrDefault(region, Collections.emptyMap());
        return new RegionalResultSnapshot(
                region,
                Instant.now(),
                strategy.consolidate(region, regionCounts)
        );
    }

    public synchronized Map<String, Map<String, Long>> conteoPorRegion() {
        Map<String, Map<String, Long>> copia = new HashMap<>();
        counts.forEach((region, votos) -> copia.put(region, Map.copyOf(votos)));
        return Map.copyOf(copia);
    }

    public synchronized Set<String> activeRegions() {
        return Set.copyOf(counts.keySet());
    }
}

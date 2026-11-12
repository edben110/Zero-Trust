package com.zerotrust.regionalrollup.domain.model;

import com.zerotrust.regionalrollup.domain.strategy.RegionalConsolidationStrategy;

import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Acumulador de votos por región.
 *
 * Mantiene conteos independientes por región y delega la consolidación
 * a una estrategia intercambiable (patrón Strategy).
 *
 * Invariantes:
 *  - Un votante solo puede votar una vez en cada región (en la práctica solo vota en una).
 *  - Los conteos internos no son accesibles directamente desde fuera.
 */
public final class RegionalVoteAccumulator {

    private final RegionalConsolidationStrategy strategy;

    // región → (voterId → ya procesado)
    private final Map<String, Set<String>>  seenVoters = new HashMap<>();
    // región → candidateId → conteo
    private final Map<String, Map<String, Long>> counts = new HashMap<>();

    public RegionalVoteAccumulator(RegionalConsolidationStrategy strategy) {
        this.strategy = strategy;
    }

    /**
     * Acepta un voto. Ignora duplicados del mismo votante.
     *
     * @return true si fue contabilizado.
     */
    public boolean accept(RawVoteEvent event) {
        String region     = event.region();
        String voterId    = event.voterId();
        String candidateId = event.candidateId();

        Set<String> voters = seenVoters.computeIfAbsent(region, k -> new HashSet<>());
        if (voters.contains(voterId)) {
            return false;
        }
        voters.add(voterId);

        Map<String, Long> regionCounts = counts.computeIfAbsent(region, k -> new HashMap<>());
        regionCounts.merge(candidateId, 1L, Long::sum);
        return true;
    }

    /**
     * Proyecta el snapshot de una región específica usando la estrategia activa.
     */
    public RegionalResultSnapshot snapshotFor(String region) {
        Map<String, Long> regionCounts = counts.getOrDefault(region, Collections.emptyMap());
        return new RegionalResultSnapshot(
                region,
                Instant.now(),
                strategy.consolidate(region, regionCounts)
        );
    }

    /**
     * Regiones con al menos un voto registrado.
     */
    public Set<String> activeRegions() {
        return Collections.unmodifiableSet(counts.keySet());
    }
}

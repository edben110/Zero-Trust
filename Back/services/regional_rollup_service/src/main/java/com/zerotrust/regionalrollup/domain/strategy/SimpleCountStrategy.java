package com.zerotrust.regionalrollup.domain.strategy;

import com.zerotrust.regionalrollup.domain.model.RegionalCandidateResult;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Estrategia de consolidación por conteo simple: devuelve los votos tal cual
 * se acumularon, ordenados de mayor a menor.
 *
 * Esta es la estrategia por defecto. Otras estrategias (ej. WeightedRegionStrategy)
 * pueden implementar RegionalConsolidationStrategy con lógica de ponderación distinta.
 */
public final class SimpleCountStrategy implements RegionalConsolidationStrategy {

    @Override
    public List<RegionalCandidateResult> consolidate(String region, Map<String, Long> rawCounts) {
        return rawCounts.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue(Comparator.reverseOrder()))
                .map(e -> new RegionalCandidateResult(e.getKey(), e.getKey(), e.getValue()))
                .collect(Collectors.toList());
    }
}

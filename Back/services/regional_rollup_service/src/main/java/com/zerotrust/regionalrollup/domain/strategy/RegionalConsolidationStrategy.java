package com.zerotrust.regionalrollup.domain.strategy;

import com.zerotrust.regionalrollup.domain.model.RegionalCandidateResult;

import java.util.List;
import java.util.Map;

/**
 * Contrato del patrón Strategy para la consolidación de votos por región.
 *
 * Distintas estrategias pueden aplicar diferentes reglas de ponderación o
 * filtrado (ej. mayoría simple, ponderación por padrón registrado, etc.)
 * sin modificar el servicio que las consume.
 */
public interface RegionalConsolidationStrategy {

    /**
     * Consolida los votos acumulados (candidateId → conteo) de una región
     * y devuelve la lista ordenada de resultados listos para publicar.
     *
     * @param region    nombre de la región
     * @param rawCounts mapa candidateId → votosTotales acumulados
     * @return lista de resultados consolidados
     */
    List<RegionalCandidateResult> consolidate(String region, Map<String, Long> rawCounts);
}

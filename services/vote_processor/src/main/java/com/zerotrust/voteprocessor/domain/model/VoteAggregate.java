package com.zerotrust.voteprocessor.domain.model;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Agregado de dominio que acumula votos globales.
 *
 * Invariantes protegidos:
 *  - Un votante solo puede votar una vez (duplicados ignorados silenciosamente).
 *  - Los contadores internos no son accesibles directamente desde fuera.
 *  - El estado solo cambia a través del método de negocio accept().
 */
public final class VoteAggregate {

    // VoterId ya visto → garantiza un voto por votante
    private final Set<String> seenVoters = new HashSet<>();

    // candidateId.value → nombre y conteo acumulado
    private final Map<String, long[]>  counts   = new HashMap<>();
    private final Map<String, String>  names    = new HashMap<>();

    /**
     * Acepta un voto. Si el voterId ya votó, el voto es ignorado (idempotencia).
     *
     * @return true si el voto fue contabilizado, false si era duplicado.
     */
    public boolean accept(RawVoteEvent event) {
        String voterId    = event.voterId().value();
        String candidateId = event.candidateId().value();

        if (seenVoters.contains(voterId)) {
            return false;
        }

        seenVoters.add(voterId);
        counts.computeIfAbsent(candidateId, k -> new long[]{0})[0]++;
        // Registra el nombre la primera vez que aparece el candidato
        names.putIfAbsent(candidateId, candidateId);
        return true;
    }

    /**
     * Proyecta el estado actual como snapshot inmutable para publicación.
     */
    public GlobalResultSnapshot snapshot() {
        List<CandidateResult> results = new ArrayList<>();
        counts.forEach((id, count) ->
                results.add(new CandidateResult(new CandidateId(id), names.get(id), count[0]))
        );
        return new GlobalResultSnapshot(Instant.now(), Collections.unmodifiableList(results));
    }

    public int totalVotes() {
        return seenVoters.size();
    }
}

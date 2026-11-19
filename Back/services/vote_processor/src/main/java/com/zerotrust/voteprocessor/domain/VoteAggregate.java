package com.zerotrust.voteprocessor.domain;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

// Thread-safe: el consumidor de Kafka y el publicador programado comparten la instancia.
public final class VoteAggregate {

    private final Set<String> seenVoters = new HashSet<>();

    private final Map<String, long[]>  counts   = new HashMap<>();
    private final Map<String, String>  names    = new HashMap<>();

    public synchronized boolean accept(RawVoteEvent event) {
        String voterId    = event.voterId().value();
        String candidateId = event.candidateId().value();

        if (seenVoters.contains(voterId)) {
            return false;
        }

        seenVoters.add(voterId);
        counts.computeIfAbsent(candidateId, k -> new long[]{0})[0]++;
        names.putIfAbsent(candidateId, candidateId);
        return true;
    }

    public synchronized GlobalResultSnapshot snapshot() {
        List<CandidateResult> results = new ArrayList<>();
        counts.forEach((id, count) ->
                results.add(new CandidateResult(new CandidateId(id), names.get(id), count[0]))
        );
        return new GlobalResultSnapshot(Instant.now(), Collections.unmodifiableList(results));
    }

    public synchronized int totalVotes() {
        return seenVoters.size();
    }
}

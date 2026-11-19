package com.zerotrust.analyticsarchiver.domain;

import java.time.Instant;
import java.util.Objects;

public record VotoAuditado(VoteId voteId, VoterId voterId, CandidateId candidateId, Region region, SourceIp sourceIp,
                           Instant votadoEn) {

    public VotoAuditado {
        Objects.requireNonNull(voteId, "voteId obligatorio");
        Objects.requireNonNull(voterId, "voterId obligatorio");
        Objects.requireNonNull(candidateId, "candidateId obligatorio");
        Objects.requireNonNull(region, "region obligatoria");
        Objects.requireNonNull(sourceIp, "sourceIp obligatoria");
        Objects.requireNonNull(votadoEn, "timestamp obligatorio");
    }
}

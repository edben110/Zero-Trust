package com.zerotrust.votingapi.domain;

import java.time.Instant;
import java.util.Objects;

// El voto que se publica en raw_votes; solo se construye después de una validación positiva.
public record VotoEmitido(VoteId voteId, VoterId voterId, CandidateId candidateId, Region region, SourceIp sourceIp,
                          Instant timestamp) {

    public VotoEmitido {
        Objects.requireNonNull(voteId, "voteId obligatorio");
        Objects.requireNonNull(voterId, "voterId obligatorio");
        Objects.requireNonNull(candidateId, "candidateId obligatorio");
        Objects.requireNonNull(region, "region obligatoria");
        Objects.requireNonNull(sourceIp, "sourceIp obligatoria");
        Objects.requireNonNull(timestamp, "timestamp obligatorio");
    }

    public static VotoEmitido emitir(SolicitudVoto solicitud, SourceIp sourceIp, VoteId voteId, Instant ahora) {
        return new VotoEmitido(voteId, solicitud.voterId(), solicitud.candidateId(), solicitud.region(), sourceIp, ahora);
    }
}

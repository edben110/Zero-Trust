package com.zerotrust.votingapi.adapter.kafka;

import com.zerotrust.votingapi.domain.VotoEmitido;

// Payload exacto de raw_votes en docs/contratos.md; Jackson respeta el orden de los componentes.
record EventoRawVote(String voteId, String voterId, String candidateId, String region, String sourceIp,
                     String timestamp) {

    static EventoRawVote de(VotoEmitido voto) {
        return new EventoRawVote(voto.voteId().valor(), voto.voterId().valor(), voto.candidateId().valor(),
                voto.region().valor(), voto.sourceIp().valor(), voto.timestamp().toString());
    }
}

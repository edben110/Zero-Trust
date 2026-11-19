package com.zerotrust.voteprocessor.repository.memoria;

import com.zerotrust.voteprocessor.domain.CandidateResult;
import com.zerotrust.voteprocessor.domain.RawVoteEvent;
import com.zerotrust.voteprocessor.domain.VoteAggregate;
import com.zerotrust.voteprocessor.port.ConteoVotos;

import java.util.List;

// Se pierde al reiniciar; sirve para probar sin PostgreSQL.
public class ConteoEnMemoria implements ConteoVotos {

    private final VoteAggregate agregado;

    public ConteoEnMemoria(VoteAggregate agregado) {
        this.agregado = agregado;
    }

    @Override
    public void registrar(RawVoteEvent voto) {
        agregado.accept(voto);
    }

    @Override
    public List<CandidateResult> conteoActual() {
        return agregado.snapshot().results();
    }
}

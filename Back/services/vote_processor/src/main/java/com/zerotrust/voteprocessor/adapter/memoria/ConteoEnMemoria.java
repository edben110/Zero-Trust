package com.zerotrust.voteprocessor.adapter.memoria;

import com.zerotrust.voteprocessor.domain.model.CandidateResult;
import com.zerotrust.voteprocessor.domain.model.RawVoteEvent;
import com.zerotrust.voteprocessor.domain.model.VoteAggregate;
import com.zerotrust.voteprocessor.domain.port.ConteoVotos;

import java.util.List;

/**
 * Conteo en memoria del proceso, útil sin PostgreSQL; se pierde al reiniciar.
 */
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

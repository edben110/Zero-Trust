package com.zerotrust.voteprocessor.domain.port;

import com.zerotrust.voteprocessor.domain.model.CandidateResult;
import com.zerotrust.voteprocessor.domain.model.RawVoteEvent;

import java.util.List;

/**
 * Puerto del conteo global: registra los votos consumidos y entrega el conteo por candidato.
 */
public interface ConteoVotos {

    void registrar(RawVoteEvent voto);

    List<CandidateResult> conteoActual();
}

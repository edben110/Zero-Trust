package com.zerotrust.voteprocessor.port;

import com.zerotrust.voteprocessor.domain.CandidateResult;
import com.zerotrust.voteprocessor.domain.RawVoteEvent;

import java.util.List;

/**
 * Puerto del conteo global: registra los votos consumidos y entrega el conteo por candidato.
 */
public interface ConteoVotos {

    void registrar(RawVoteEvent voto);

    List<CandidateResult> conteoActual();
}

package com.zerotrust.regionalrollup.domain.port;

import com.zerotrust.regionalrollup.domain.model.RawVoteEvent;

import java.util.Map;

/**
 * Puerto del conteo regional: registra los votos consumidos y entrega región → candidateId → votos.
 */
public interface ConteoVotosRegionales {

    void registrar(RawVoteEvent voto);

    Map<String, Map<String, Long>> conteoPorRegion();
}

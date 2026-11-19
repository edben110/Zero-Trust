package com.zerotrust.regionalrollup.port;

import com.zerotrust.regionalrollup.domain.RawVoteEvent;

import java.util.Map;

public interface ConteoVotosRegionales {

    void registrar(RawVoteEvent voto);

    Map<String, Map<String, Long>> conteoPorRegion();
}

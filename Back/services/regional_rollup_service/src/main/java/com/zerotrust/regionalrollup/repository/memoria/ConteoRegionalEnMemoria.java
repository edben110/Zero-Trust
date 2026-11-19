package com.zerotrust.regionalrollup.repository.memoria;

import com.zerotrust.regionalrollup.domain.RawVoteEvent;
import com.zerotrust.regionalrollup.domain.RegionalVoteAccumulator;
import com.zerotrust.regionalrollup.port.ConteoVotosRegionales;

import java.util.Map;

/**
 * Conteo regional en memoria del proceso, útil sin PostgreSQL; se pierde al reiniciar.
 */
public class ConteoRegionalEnMemoria implements ConteoVotosRegionales {

    private final RegionalVoteAccumulator acumulador;

    public ConteoRegionalEnMemoria(RegionalVoteAccumulator acumulador) {
        this.acumulador = acumulador;
    }

    @Override
    public void registrar(RawVoteEvent voto) {
        acumulador.accept(voto);
    }

    @Override
    public Map<String, Map<String, Long>> conteoPorRegion() {
        return acumulador.conteoPorRegion();
    }
}

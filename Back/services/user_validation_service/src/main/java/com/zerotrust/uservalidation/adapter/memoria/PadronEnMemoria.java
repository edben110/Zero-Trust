package com.zerotrust.uservalidation.adapter.memoria;

import com.zerotrust.uservalidation.domain.PadronElectoral;
import com.zerotrust.uservalidation.domain.VoterId;
import com.zerotrust.uservalidation.port.PadronVotantes;

public class PadronEnMemoria implements PadronVotantes {

    private final PadronElectoral padron;

    public PadronEnMemoria(PadronElectoral padron) {
        this.padron = padron;
    }

    @Override
    public void habilitar(VoterId voterId) {
        padron.habilitar(voterId);
    }

    @Override
    public void retirar(VoterId voterId) {
        padron.retirar(voterId);
    }

    @Override
    public boolean estaHabilitado(VoterId voterId) {
        return padron.estaHabilitado(voterId);
    }
}

package com.zerotrust.uservalidation.service;

import com.zerotrust.uservalidation.domain.VoterId;
import com.zerotrust.uservalidation.port.PadronVotantes;

public class ServicioPadronElectoral {

    private final PadronVotantes padron;

    public ServicioPadronElectoral(PadronVotantes padron) {
        this.padron = padron;
    }

    public void registrarElegible(VoterId voterId) {
        padron.habilitar(voterId);
    }

    public void retirar(VoterId voterId) {
        padron.retirar(voterId);
    }

    public boolean esElegible(VoterId voterId) {
        return padron.estaHabilitado(voterId);
    }
}

package com.zerotrust.votingapi.port;

import com.zerotrust.votingapi.domain.VoterId;

public interface ValidadorVotante {

    // Lanza ValidacionNoDisponibleException si no hay respuesta a tiempo.
    boolean estaHabilitado(VoterId voterId);
}

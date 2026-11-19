package com.zerotrust.votingapi.port;

import com.zerotrust.votingapi.domain.VotanteInscrito;

public interface RepositorioPadron {

    // Lanza VotanteYaInscritoException si ya estaba; debe correr dentro de EjecutorTransaccional.
    void inscribir(VotanteInscrito votante);
}

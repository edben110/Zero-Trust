package com.zerotrust.votingapi.port;

import com.zerotrust.votingapi.domain.VotanteInscrito;

public interface RepositorioPadron {

    // Inserta el votante en registered_voters. Lanza VotanteYaInscritoException si su voterId ya estaba.
    // Debe ejecutarse dentro de la transacción de EjecutorTransaccional.
    void inscribir(VotanteInscrito votante);
}

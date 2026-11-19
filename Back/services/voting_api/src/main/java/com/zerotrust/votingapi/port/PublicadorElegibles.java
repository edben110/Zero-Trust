package com.zerotrust.votingapi.port;

import com.zerotrust.votingapi.domain.VotanteInscrito;

public interface PublicadorElegibles {

    // Lanza PublicacionNoDisponibleException si el broker no confirma el alta.
    void publicar(VotanteInscrito votante);
}

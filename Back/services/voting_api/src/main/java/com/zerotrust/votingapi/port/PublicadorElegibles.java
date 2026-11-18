package com.zerotrust.votingapi.port;

import com.zerotrust.votingapi.domain.VotanteInscrito;

public interface PublicadorElegibles {

    // Publica el alta en eligible_voters para que user_validation_service la habilite.
    // Lanza PublicacionNoDisponibleException si el broker no confirma.
    void publicar(VotanteInscrito votante);
}

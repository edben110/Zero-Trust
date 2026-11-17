package com.zerotrust.votingapi.port;

import com.zerotrust.votingapi.domain.VotoEmitido;

public interface PublicadorVotos {

    // Lanza PublicacionNoDisponibleException si el voto no queda confirmado en el broker.
    void publicar(VotoEmitido voto);
}

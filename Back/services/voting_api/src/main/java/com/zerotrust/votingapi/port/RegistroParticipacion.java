package com.zerotrust.votingapi.port;

import com.zerotrust.votingapi.domain.ElectionId;
import com.zerotrust.votingapi.domain.VoterId;

public interface RegistroParticipacion {

    // Lanza VotoDuplicadoException si ya votó; debe correr dentro de EjecutorTransaccional.
    void registrar(ElectionId eleccion, VoterId voterId);
}

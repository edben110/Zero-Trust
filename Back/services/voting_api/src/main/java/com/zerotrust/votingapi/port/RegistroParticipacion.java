package com.zerotrust.votingapi.port;

import com.zerotrust.votingapi.domain.ElectionId;
import com.zerotrust.votingapi.domain.VoterId;

public interface RegistroParticipacion {

    // Registra que el VoterId votó en la elección. Lanza VotoDuplicadoException si ya estaba registrado.
    // Debe ejecutarse dentro de la transacción de EjecutorTransaccional.
    void registrar(ElectionId eleccion, VoterId voterId);
}

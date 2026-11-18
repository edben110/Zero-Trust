package com.zerotrust.votingapi.port;

import com.zerotrust.votingapi.domain.Cedula;
import com.zerotrust.votingapi.domain.ElectionId;
import com.zerotrust.votingapi.domain.VoterId;

public interface DerivadorVoterId {

    // Determinista: la misma cédula en la misma elección da siempre el mismo VoterId.
    VoterId derivar(Cedula cedula, ElectionId eleccion);
}

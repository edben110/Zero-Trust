package com.zerotrust.voterregistration.port;

import com.zerotrust.voterregistration.domain.Cedula;
import com.zerotrust.voterregistration.domain.ElectionId;
import com.zerotrust.voterregistration.domain.VoterId;

public interface DerivadorVoterId {

    // Determinista: la misma cédula en la misma elección da siempre el mismo VoterId.
    VoterId derivar(Cedula cedula, ElectionId eleccion);
}

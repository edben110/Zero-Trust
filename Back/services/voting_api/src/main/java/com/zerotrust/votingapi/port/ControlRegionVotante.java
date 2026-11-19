package com.zerotrust.votingapi.port;

import com.zerotrust.votingapi.domain.ElectionId;
import com.zerotrust.votingapi.domain.Region;
import com.zerotrust.votingapi.domain.ResultadoRegion;
import com.zerotrust.votingapi.domain.VoterId;

public interface ControlRegionVotante {

    // Debe correr en la transacción del voto; los errores de región se devuelven, no se lanzan, para que persistan los fallos.
    ResultadoRegion evaluar(ElectionId eleccion, VoterId votante, Region seleccionada);
}

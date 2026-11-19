package com.zerotrust.votingapi.port;

import com.zerotrust.votingapi.domain.ElectionId;
import com.zerotrust.votingapi.domain.Region;
import com.zerotrust.votingapi.domain.ResultadoRegion;
import com.zerotrust.votingapi.domain.VoterId;

public interface ControlRegionVotante {

    // Debe ejecutarse dentro de la transacción del voto. Los errores de región
    // se devuelven como resultado, no como excepción: así persisten los fallos.
    ResultadoRegion evaluar(ElectionId eleccion, VoterId votante, Region seleccionada);
}

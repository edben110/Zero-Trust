package com.zerotrust.votingapi.port;

import com.zerotrust.votingapi.domain.ElectionId;
import com.zerotrust.votingapi.domain.Region;
import com.zerotrust.votingapi.domain.VoterId;

public interface ControlRegionVotante {
    enum Resultado { CORRECTA, PRIMER_ERROR, ANULADO }

    // Debe ejecutarse dentro de la transacción del voto. Los errores de región
    // se devuelven como resultado, no como excepción: así persisten los fallos.
    Resultado evaluar(ElectionId eleccion, VoterId votante, Region seleccionada);
}

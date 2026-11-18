package com.zerotrust.votingapi.service;

import com.zerotrust.votingapi.domain.Cedula;
import com.zerotrust.votingapi.domain.ElectionId;
import com.zerotrust.votingapi.domain.VoterId;
import com.zerotrust.votingapi.port.DerivadorVoterId;

// Devuelve el VoterId con el que la persona vota en la elección en curso. No guarda ni registra la cédula.
// Responde igual esté o no la cédula en el padrón, para no revelar quién está inscrito:
// la habilitación se comprueba al votar.
public class ServicioIdentificacion {

    private final DerivadorVoterId derivador;
    private final ElectionId eleccion;

    public ServicioIdentificacion(DerivadorVoterId derivador, ElectionId eleccion) {
        this.derivador = derivador;
        this.eleccion = eleccion;
    }

    public VoterId identificar(Cedula cedula) {
        return derivador.derivar(cedula, eleccion);
    }
}

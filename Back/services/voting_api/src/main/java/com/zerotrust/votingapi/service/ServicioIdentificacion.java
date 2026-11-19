package com.zerotrust.votingapi.service;

import com.zerotrust.votingapi.domain.Cedula;
import com.zerotrust.votingapi.domain.ElectionId;
import com.zerotrust.votingapi.exception.VotanteFueraDelPadronException;
import com.zerotrust.votingapi.domain.VoterId;
import com.zerotrust.votingapi.port.DerivadorVoterId;
import com.zerotrust.votingapi.port.ValidadorVotante;

// Devuelve el VoterId con el que la persona vota en la elección en curso. No guarda ni registra la cédula.
// Antes de entregarlo comprueba el padrón con la misma fuente que usa el voto (user_validation_service):
// si el titular no está inscrito, lo avisa en lugar de dar un código que no podría votar.
public class ServicioIdentificacion {

    private final DerivadorVoterId derivador;
    private final ValidadorVotante validador;
    private final ElectionId eleccion;

    public ServicioIdentificacion(DerivadorVoterId derivador, ValidadorVotante validador, ElectionId eleccion) {
        this.derivador = derivador;
        this.validador = validador;
        this.eleccion = eleccion;
    }

    public VoterId identificar(Cedula cedula) {
        VoterId voterId = derivador.derivar(cedula, eleccion);
        if (!validador.estaHabilitado(voterId)) {
            throw new VotanteFueraDelPadronException();
        }
        return voterId;
    }
}

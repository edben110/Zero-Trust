package com.zerotrust.votingapi.service;

import com.zerotrust.votingapi.domain.ComprobanteVoto;
import com.zerotrust.votingapi.domain.SolicitudVoto;
import com.zerotrust.votingapi.domain.SourceIp;
import com.zerotrust.votingapi.domain.VotanteNoHabilitadoException;
import com.zerotrust.votingapi.domain.VoteId;
import com.zerotrust.votingapi.domain.VotoEmitido;
import com.zerotrust.votingapi.port.PublicadorVotos;
import com.zerotrust.votingapi.port.ValidadorVotante;

import java.time.Clock;
import java.util.UUID;
import java.util.function.Supplier;

public class ServicioVotacion {

    private final ValidadorVotante validador;
    private final PublicadorVotos publicador;
    private final Clock reloj;
    private final Supplier<UUID> generadorIds;

    public ServicioVotacion(ValidadorVotante validador, PublicadorVotos publicador, Clock reloj,
                            Supplier<UUID> generadorIds) {
        this.validador = validador;
        this.publicador = publicador;
        this.reloj = reloj;
        this.generadorIds = generadorIds;
    }

    // Nunca se publica un voto sin validación positiva: la publicación va después de la respuesta RPC.
    public ComprobanteVoto votar(SolicitudVoto solicitud, SourceIp sourceIp) {
        if (!validador.estaHabilitado(solicitud.voterId())) {
            throw new VotanteNoHabilitadoException();
        }
        VotoEmitido voto = VotoEmitido.emitir(solicitud, sourceIp, VoteId.de(generadorIds.get()), reloj.instant());
        publicador.publicar(voto);
        return new ComprobanteVoto(voto.voteId(), voto.timestamp());
    }
}

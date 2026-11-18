package com.zerotrust.votingapi.service;

import java.time.Clock;
import java.util.UUID;
import java.util.function.Supplier;

import com.zerotrust.votingapi.domain.ComprobanteVoto;
import com.zerotrust.votingapi.domain.ElectionId;
import com.zerotrust.votingapi.domain.SolicitudVoto;
import com.zerotrust.votingapi.domain.SourceIp;
import com.zerotrust.votingapi.domain.VotanteNoHabilitadoException;
import com.zerotrust.votingapi.domain.VoteId;
import com.zerotrust.votingapi.domain.VotoEmitido;
import com.zerotrust.votingapi.port.EjecutorTransaccional;
import com.zerotrust.votingapi.port.PublicadorVotos;
import com.zerotrust.votingapi.port.RegistroParticipacion;
import com.zerotrust.votingapi.port.ValidadorVotante;

public class ServicioVotacion {

    private final ValidadorVotante validador;
    private final PublicadorVotos publicador;
    private final RegistroParticipacion registro;
    private final EjecutorTransaccional transaccion;
    private final ElectionId eleccion;
    private final Clock reloj;
    private final Supplier<UUID> generadorIds;

    public ServicioVotacion(ValidadorVotante validador, PublicadorVotos publicador, RegistroParticipacion registro,
                            EjecutorTransaccional transaccion, ElectionId eleccion, Clock reloj,
                            Supplier<UUID> generadorIds) {
        this.validador = validador;
        this.publicador = publicador;
        this.registro = registro;
        this.transaccion = transaccion;
        this.eleccion = eleccion;
        this.reloj = reloj;
        this.generadorIds = generadorIds;
    }

    // Nunca se publica un voto sin validación positiva: la publicación va después de la respuesta RPC.
    // La validación RPC va fuera de la transacción para no retener una conexión de base de datos esperando.
    // Registro de uso y publicación forman una sola unidad: si la publicación falla, el registro se revierte
    // y la persona puede reintentar. Una segunda solicitud con el mismo voterId espera en la restricción única.
    public ComprobanteVoto votar(SolicitudVoto solicitud, SourceIp sourceIp) {
        if (!validador.estaHabilitado(solicitud.voterId())) {
            throw new VotanteNoHabilitadoException();
        }
        VotoEmitido voto = VotoEmitido.emitir(solicitud, sourceIp, VoteId.de(generadorIds.get()), reloj.instant());
        return transaccion.ejecutar(() -> {
            registro.registrar(eleccion, solicitud.voterId());
            publicador.publicar(voto);
            return new ComprobanteVoto(voto.voteId(), voto.timestamp());
        });
    }
}

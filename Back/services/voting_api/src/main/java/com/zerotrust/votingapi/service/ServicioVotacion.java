package com.zerotrust.votingapi.service;

import java.time.Clock;
import java.util.UUID;
import java.util.function.Supplier;

import com.zerotrust.votingapi.domain.ComprobanteVoto;
import com.zerotrust.votingapi.domain.ElectionId;
import com.zerotrust.votingapi.domain.FraudKind;
import com.zerotrust.votingapi.domain.SolicitudVoto;
import com.zerotrust.votingapi.domain.SourceIp;
import com.zerotrust.votingapi.exception.RegionIncorrectaException;
import com.zerotrust.votingapi.exception.VotanteNoHabilitadoException;
import com.zerotrust.votingapi.exception.VotoAnuladoException;
import com.zerotrust.votingapi.exception.VotoDuplicadoException;
import com.zerotrust.votingapi.domain.VoteId;
import com.zerotrust.votingapi.domain.VotoEmitido;
import com.zerotrust.votingapi.port.EjecutorTransaccional;
import com.zerotrust.votingapi.port.ControlRegionVotante;
import com.zerotrust.votingapi.port.PublicadorVotos;
import com.zerotrust.votingapi.port.RegistroIncidentesFraude;
import com.zerotrust.votingapi.port.RegistroParticipacion;
import com.zerotrust.votingapi.port.RegistroVotoAceptado;
import com.zerotrust.votingapi.port.ValidadorVotante;

public class ServicioVotacion {

    private final ValidadorVotante validador;
    private final PublicadorVotos publicador;
    private final RegistroParticipacion registro;
    private final RegistroVotoAceptado aceptados;
    private final ControlRegionVotante controlRegion;
    private final RegistroIncidentesFraude incidentes;
    private final EjecutorTransaccional transaccion;
    private final ElectionId eleccion;
    private final Clock reloj;
    private final Supplier<UUID> generadorIds;

    public ServicioVotacion(ValidadorVotante validador, PublicadorVotos publicador, RegistroParticipacion registro,
                            RegistroVotoAceptado aceptados, ControlRegionVotante controlRegion,
                            RegistroIncidentesFraude incidentes, EjecutorTransaccional transaccion,
                            ElectionId eleccion, Clock reloj, Supplier<UUID> generadorIds) {
        this.validador = validador;
        this.publicador = publicador;
        this.registro = registro;
        this.aceptados = aceptados;
        this.controlRegion = controlRegion;
        this.incidentes = incidentes;
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
        ResultadoTransaccion resultado;
        try {
            resultado = transaccion.ejecutar(() -> {
                ControlRegionVotante.Resultado region = controlRegion.evaluar(eleccion, solicitud.voterId(), solicitud.region());
                if (region != ControlRegionVotante.Resultado.CORRECTA) {
                    return new ResultadoTransaccion(null, region);
                }
                registro.registrar(eleccion, solicitud.voterId());
                aceptados.registrar(voto);
                publicador.publicar(voto);
                return new ResultadoTransaccion(new ComprobanteVoto(voto.voteId(), voto.timestamp()), region);
            });
        } catch (VotoDuplicadoException e) {
            // Ya se emitió un voto con este documento: es un incidente, no un error de red.
            // Se reporta fuera de la transacción para que el rollback no lo descarte.
            incidentes.registrar(FraudKind.DUPLICATE_VOTE, sourceIp);
            throw e;
        }
        // Fuera de la transacción: lanzarlas adentro revertiría el contador de errores.
        if (resultado.region() == ControlRegionVotante.Resultado.PRIMER_ERROR) throw new RegionIncorrectaException();
        if (resultado.region() == ControlRegionVotante.Resultado.ANULADO) {
            // Dos regiones distintas de la registrada: el voto queda anulado y se audita como fraude.
            incidentes.registrar(FraudKind.WRONG_REGION, sourceIp);
            throw new VotoAnuladoException();
        }
        return resultado.comprobante();
    }

    private record ResultadoTransaccion(ComprobanteVoto comprobante, ControlRegionVotante.Resultado region) { }
}

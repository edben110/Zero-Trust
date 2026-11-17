package com.zerotrust.votingapi.service;

import com.zerotrust.votingapi.domain.Candidato;
import com.zerotrust.votingapi.domain.CandidateId;
import com.zerotrust.votingapi.domain.Catalogo;
import com.zerotrust.votingapi.domain.ComprobanteVoto;
import com.zerotrust.votingapi.domain.PublicacionNoDisponibleException;
import com.zerotrust.votingapi.domain.Region;
import com.zerotrust.votingapi.domain.SolicitudVoto;
import com.zerotrust.votingapi.domain.SourceIp;
import com.zerotrust.votingapi.domain.ValidacionNoDisponibleException;
import com.zerotrust.votingapi.domain.VotanteNoHabilitadoException;
import com.zerotrust.votingapi.domain.VoteId;
import com.zerotrust.votingapi.domain.VoterId;
import com.zerotrust.votingapi.domain.VotoEmitido;
import com.zerotrust.votingapi.port.PublicadorVotos;
import com.zerotrust.votingapi.port.ValidadorVotante;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ServicioVotacionTest {

    private static final String VOTANTE = "3f2b8c1e-6d4a-4b7e-9c21-0a5d6e7f8a9b";
    private static final String CANDIDATO = "0b1c2d3e-4f50-4a61-8b72-93a4b5c6d7e8";
    private static final UUID VOTE_ID = UUID.fromString("11111111-2222-4333-8444-555555555555");
    private static final Instant AHORA = Instant.parse("2026-11-14T15:00:00Z");
    private static final SourceIp IP = new SourceIp("203.0.113.7");

    @Mock
    private ValidadorVotante validador;
    @Mock
    private PublicadorVotos publicador;

    private ServicioVotacion servicio;
    private SolicitudVoto solicitud;

    @BeforeEach
    void construir() {
        servicio = new ServicioVotacion(validador, publicador, Clock.fixed(AHORA, ZoneOffset.UTC), () -> VOTE_ID);
        Catalogo catalogo = new Catalogo(List.of(new Candidato(new CandidateId(CANDIDATO), "A", "B")),
                List.of(new Region("norte")));
        solicitud = SolicitudVoto.desde(VOTANTE, CANDIDATO, "norte", catalogo);
    }

    @Test
    @DisplayName("Con validación positiva debe publicar exactamente un voto y devolver el comprobante")
    void caminoFeliz() {
        // Arrange
        when(validador.estaHabilitado(new VoterId(VOTANTE))).thenReturn(true);
        ArgumentCaptor<VotoEmitido> voto = ArgumentCaptor.forClass(VotoEmitido.class);

        // Act
        ComprobanteVoto comprobante = servicio.votar(solicitud, IP);

        // Assert
        verify(publicador, times(1)).publicar(voto.capture());
        assertThat(voto.getValue().voteId()).isEqualTo(VoteId.de(VOTE_ID));
        assertThat(voto.getValue().voterId()).isEqualTo(new VoterId(VOTANTE));
        assertThat(voto.getValue().sourceIp()).isEqualTo(IP);
        assertThat(voto.getValue().timestamp()).isEqualTo(AHORA);
        assertThat(comprobante.voteId()).isEqualTo(VoteId.de(VOTE_ID));
        assertThat(comprobante.recibidoEn()).isEqualTo(AHORA);
    }

    @Test
    @DisplayName("Debe validar antes de publicar")
    void validaAntesDePublicar() {
        when(validador.estaHabilitado(any())).thenReturn(true);

        servicio.votar(solicitud, IP);

        InOrder orden = inOrder(validador, publicador);
        orden.verify(validador).estaHabilitado(new VoterId(VOTANTE));
        orden.verify(publicador).publicar(any());
    }

    @Test
    @DisplayName("Un votante no habilitado no debe publicar nada")
    void noHabilitado() {
        when(validador.estaHabilitado(any())).thenReturn(false);

        assertThrows(VotanteNoHabilitadoException.class, () -> servicio.votar(solicitud, IP));

        verify(publicador, never()).publicar(any());
    }

    @Test
    @DisplayName("Sin respuesta de validación no debe publicar nada")
    void validacionNoDisponible() {
        when(validador.estaHabilitado(any())).thenThrow(new ValidacionNoDisponibleException());

        assertThrows(ValidacionNoDisponibleException.class, () -> servicio.votar(solicitud, IP));

        verify(publicador, never()).publicar(any());
    }

    @Test
    @DisplayName("Si la publicación falla, el error se propaga como no disponible")
    void publicacionFalla() {
        when(validador.estaHabilitado(any())).thenReturn(true);
        doThrow(new PublicacionNoDisponibleException()).when(publicador).publicar(any());

        assertThrows(PublicacionNoDisponibleException.class, () -> servicio.votar(solicitud, IP));

        verify(publicador, times(1)).publicar(any());
        verifyNoMoreInteractions(publicador);
    }
}

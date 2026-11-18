package com.zerotrust.votingapi.service;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.InOrder;
import org.mockito.Mock;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

import com.zerotrust.votingapi.adapter.crypto.DerivadorVoterIdHmac;
import com.zerotrust.votingapi.domain.CandidateId;
import com.zerotrust.votingapi.domain.Candidato;
import com.zerotrust.votingapi.domain.Catalogo;
import com.zerotrust.votingapi.domain.Cedula;
import com.zerotrust.votingapi.domain.ComprobanteVoto;
import com.zerotrust.votingapi.domain.ElectionId;
import com.zerotrust.votingapi.domain.PublicacionNoDisponibleException;
import com.zerotrust.votingapi.domain.Region;
import com.zerotrust.votingapi.domain.SolicitudVoto;
import com.zerotrust.votingapi.domain.SourceIp;
import com.zerotrust.votingapi.domain.ValidacionNoDisponibleException;
import com.zerotrust.votingapi.domain.VotanteNoHabilitadoException;
import com.zerotrust.votingapi.domain.VoteId;
import com.zerotrust.votingapi.domain.VoterId;
import com.zerotrust.votingapi.domain.VotoDuplicadoException;
import com.zerotrust.votingapi.domain.VotoEmitido;
import com.zerotrust.votingapi.port.EjecutorTransaccional;
import com.zerotrust.votingapi.port.PublicadorVotos;
import com.zerotrust.votingapi.port.RegistroParticipacion;
import com.zerotrust.votingapi.port.ValidadorVotante;

class ServicioVotacionTest {

    private static final String VOTANTE = "3f2b8c1e-6d4a-4b7e-9c21-0a5d6e7f8a9b";
    private static final String CANDIDATO = "0b1c2d3e-4f50-4a61-8b72-93a4b5c6d7e8";
    private static final UUID VOTE_ID = UUID.fromString("11111111-2222-4333-8444-555555555555");
    private static final Instant AHORA = Instant.parse("2026-11-14T15:00:00Z");
    private static final SourceIp IP = new SourceIp("203.0.113.7");
    private static final ElectionId ELECCION = new ElectionId("elecciones-2026");

    private static SolicitudVoto solicitud(String voterId) {
        Catalogo catalogo = new Catalogo(List.of(new Candidato(new CandidateId(CANDIDATO), "A", "B")),
                List.of(new Region("norte")));
        return SolicitudVoto.desde(voterId, CANDIDATO, "norte", catalogo);
    }

    // Aproxima PostgreSQL: el conjunto es la restricción UNIQUE y, si la acción lanza una excepción,
    // se deshacen los registros hechos dentro de ella (rollback).
    static class AlmacenEnMemoria implements RegistroParticipacion, EjecutorTransaccional {
        final Set<String> usados = ConcurrentHashMap.newKeySet();
        private final ThreadLocal<List<String>> deLaTransaccion = ThreadLocal.withInitial(ArrayList::new);

        @Override
        public void registrar(ElectionId eleccion, VoterId voterId) {
            String clave = eleccion.valor() + "|" + voterId.valor();
            if (!usados.add(clave)) {
                throw new VotoDuplicadoException();
            }
            deLaTransaccion.get().add(clave);
        }

        @Override
        public <T> T ejecutar(Supplier<T> accion) {
            try {
                return accion.get();
            } catch (RuntimeException e) {
                deLaTransaccion.get().forEach(usados::remove);
                throw e;
            } finally {
                deLaTransaccion.remove();
            }
        }
    }

    @Nested
    @ExtendWith(MockitoExtension.class)
    @DisplayName("Orquestación con dobles (Mockito)")
    class Orquestacion {

        @Mock
        private ValidadorVotante validador;
        @Mock
        private PublicadorVotos publicador;
        @Mock
        private RegistroParticipacion registro;
        @Mock
        private EjecutorTransaccional transaccion;

        private ServicioVotacion servicio;

        @BeforeEach
        void construir() {
            servicio = new ServicioVotacion(validador, publicador, registro, transaccion, ELECCION,
                    Clock.fixed(AHORA, ZoneOffset.UTC), () -> VOTE_ID);
        }

        @SuppressWarnings("unchecked")
        private void transaccionEjecutaLaAccion() {
            when(transaccion.ejecutar(any(Supplier.class))).thenAnswer(i -> ((Supplier<?>) i.getArgument(0)).get());
        }

        @Test
        @DisplayName("Con validación positiva debe registrar el voterId y publicar exactamente un voto")
        void caminoFeliz() {
            // Arrange
            when(validador.estaHabilitado(new VoterId(VOTANTE))).thenReturn(true);
            transaccionEjecutaLaAccion();
            ArgumentCaptor<VotoEmitido> voto = ArgumentCaptor.forClass(VotoEmitido.class);

            // Act
            ComprobanteVoto comprobante = servicio.votar(solicitud(VOTANTE), IP);

            // Assert
            verify(registro, times(1)).registrar(ELECCION, new VoterId(VOTANTE));
            verify(publicador, times(1)).publicar(voto.capture());
            assertThat(voto.getValue().voteId()).isEqualTo(VoteId.de(VOTE_ID));
            assertThat(voto.getValue().voterId()).isEqualTo(new VoterId(VOTANTE));
            assertThat(voto.getValue().sourceIp()).isEqualTo(IP);
            assertThat(voto.getValue().timestamp()).isEqualTo(AHORA);
            assertThat(comprobante.voteId()).isEqualTo(VoteId.de(VOTE_ID));
            assertThat(comprobante.recibidoEn()).isEqualTo(AHORA);
        }

        @Test
        @DisplayName("Debe validar antes de la transacción, y registrar antes de publicar")
        void orden() {
            when(validador.estaHabilitado(any())).thenReturn(true);
            transaccionEjecutaLaAccion();

            servicio.votar(solicitud(VOTANTE), IP);

            InOrder orden = inOrder(validador, transaccion, registro, publicador);
            orden.verify(validador).estaHabilitado(new VoterId(VOTANTE));
            orden.verify(transaccion).ejecutar(any());
            orden.verify(registro).registrar(ELECCION, new VoterId(VOTANTE));
            orden.verify(publicador).publicar(any());
        }

        @Test
        @DisplayName("Un votante no habilitado no debe registrar ni publicar nada")
        void noHabilitado() {
            when(validador.estaHabilitado(any())).thenReturn(false);

            assertThrows(VotanteNoHabilitadoException.class, () -> servicio.votar(solicitud(VOTANTE), IP));

            verifyNoInteractions(registro, publicador, transaccion);
        }

        @Test
        @DisplayName("Sin respuesta de validación no debe registrar ni publicar nada")
        void validacionNoDisponible() {
            when(validador.estaHabilitado(any())).thenThrow(new ValidacionNoDisponibleException());

            assertThrows(ValidacionNoDisponibleException.class, () -> servicio.votar(solicitud(VOTANTE), IP));

            verifyNoInteractions(registro, publicador, transaccion);
        }

        @Test
        @DisplayName("Si el voterId ya votó, no debe publicar")
        void duplicado() {
            when(validador.estaHabilitado(any())).thenReturn(true);
            transaccionEjecutaLaAccion();
            doThrow(new VotoDuplicadoException()).when(registro).registrar(any(), any());

            assertThrows(VotoDuplicadoException.class, () -> servicio.votar(solicitud(VOTANTE), IP));

            verify(publicador, never()).publicar(any());
        }

        @Test
        @DisplayName("Si la publicación falla, el error se propaga dentro de la transacción para que se revierta")
        void publicacionFalla() {
            when(validador.estaHabilitado(any())).thenReturn(true);
            transaccionEjecutaLaAccion();
            doThrow(new PublicacionNoDisponibleException()).when(publicador).publicar(any());

            assertThrows(PublicacionNoDisponibleException.class, () -> servicio.votar(solicitud(VOTANTE), IP));

            verify(registro, times(1)).registrar(ELECCION, new VoterId(VOTANTE));
            verify(publicador, times(1)).publicar(any());
        }
    }

    @Nested
    @ExtendWith(MockitoExtension.class)
    @DisplayName("Voto único con un almacén en memoria")
    class VotoUnico {

        @Mock
        private ValidadorVotante validador;
        @Mock
        private PublicadorVotos publicador;

        private AlmacenEnMemoria almacen;

        @BeforeEach
        void construir() {
            almacen = new AlmacenEnMemoria();
        }

        private ServicioVotacion servicioDe(ElectionId eleccion) {
            return new ServicioVotacion(validador, publicador, almacen, almacen, eleccion,
                    Clock.fixed(AHORA, ZoneOffset.UTC), UUID::randomUUID);
        }

        @Test
        @DisplayName("Un voterId puede votar una vez; el segundo intento se rechaza sin publicar")
        void unaVez() {
            when(validador.estaHabilitado(any())).thenReturn(true);
            ServicioVotacion servicio = servicioDe(ELECCION);

            servicio.votar(solicitud(VOTANTE), IP);
            assertThrows(VotoDuplicadoException.class, () -> servicio.votar(solicitud(VOTANTE), IP));

            verify(publicador, times(1)).publicar(any());
        }

        @Test
        @DisplayName("El voterId de la misma cédula en otra elección puede votar una vez")
        void otraEleccion() {
            when(validador.estaHabilitado(any())).thenReturn(true);
            DerivadorVoterIdHmac derivador = new DerivadorVoterIdHmac(new byte[32]);
            ElectionId otra = new ElectionId("elecciones-2027");
            String en2026 = derivador.derivar(new Cedula("1020304051"), ELECCION).valor();
            String en2027 = derivador.derivar(new Cedula("1020304051"), otra).valor();

            servicioDe(ELECCION).votar(solicitud(en2026), IP);
            servicioDe(otra).votar(solicitud(en2027), IP);

            verify(publicador, times(2)).publicar(any());
            assertThrows(VotoDuplicadoException.class, () -> servicioDe(otra).votar(solicitud(en2027), IP));
        }

        @Test
        @DisplayName("Si falla el guardado del voto, se revierte el registro y la persona puede reintentar")
        void reintentoTrasFallo() {
            when(validador.estaHabilitado(any())).thenReturn(true);
            doThrow(new PublicacionNoDisponibleException()).doNothing().when(publicador).publicar(any());
            ServicioVotacion servicio = servicioDe(ELECCION);

            assertThrows(PublicacionNoDisponibleException.class, () -> servicio.votar(solicitud(VOTANTE), IP));
            assertThat(almacen.usados).isEmpty();

            assertThat(servicio.votar(solicitud(VOTANTE), IP)).isNotNull();
            assertThat(almacen.usados).hasSize(1);
            verify(publicador, times(2)).publicar(any());
        }

        @Test
        @DisplayName("Con solicitudes simultáneas para el mismo voterId, solo una guarda el voto")
        void simultaneas() throws Exception {
            when(validador.estaHabilitado(any())).thenReturn(true);
            AtomicInteger publicados = new AtomicInteger();
            doAnswer(i -> {
                publicados.incrementAndGet();
                return null;
            }).when(publicador).publicar(any());
            ServicioVotacion servicio = servicioDe(ELECCION);
            int hilos = 16;
            CountDownLatch salida = new CountDownLatch(1);
            ExecutorService pool = Executors.newFixedThreadPool(hilos);
            List<Future<Boolean>> resultados = new ArrayList<>();

            for (int i = 0; i < hilos; i++) {
                resultados.add(pool.submit(() -> {
                    salida.await();
                    try {
                        servicio.votar(solicitud(VOTANTE), IP);
                        return true;
                    } catch (VotoDuplicadoException e) {
                        return false;
                    }
                }));
            }
            salida.countDown();
            int aceptados = 0;
            for (Future<Boolean> resultado : resultados) {
                if (resultado.get(10, TimeUnit.SECONDS)) {
                    aceptados++;
                }
            }
            pool.shutdownNow();

            assertThat(aceptados).isEqualTo(1);
            assertThat(publicados.get()).isEqualTo(1);
            assertThat(almacen.usados).hasSize(1);
        }
    }
}

package com.zerotrust.votingapi.service;

import com.zerotrust.votingapi.adapter.crypto.DerivadorVoterIdHmac;
import com.zerotrust.votingapi.domain.Candidato;
import com.zerotrust.votingapi.domain.CandidateId;
import com.zerotrust.votingapi.domain.Catalogo;
import com.zerotrust.votingapi.domain.Cedula;
import com.zerotrust.votingapi.domain.ElectionId;
import com.zerotrust.votingapi.domain.PublicacionNoDisponibleException;
import com.zerotrust.votingapi.domain.Region;
import com.zerotrust.votingapi.domain.SolicitudVotoInvalidaException;
import com.zerotrust.votingapi.domain.VotanteInscrito;
import com.zerotrust.votingapi.domain.VotanteYaInscritoException;
import com.zerotrust.votingapi.domain.VoterId;
import com.zerotrust.votingapi.port.EjecutorTransaccional;
import com.zerotrust.votingapi.port.PublicadorElegibles;
import com.zerotrust.votingapi.port.RepositorioPadron;
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
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ServicioPadronTest {

    private static final ElectionId ELECCION = new ElectionId("elecciones-2026");
    private static final Instant AHORA = Instant.parse("2026-11-18T10:00:00Z");
    private static final DerivadorVoterIdHmac DERIVADOR = new DerivadorVoterIdHmac(clavePrueba());

    @Mock
    private RepositorioPadron repositorio;
    @Mock
    private PublicadorElegibles publicador;
    @Mock
    private EjecutorTransaccional transaccion;

    private ServicioPadron servicio;

    private static byte[] clavePrueba() {
        byte[] clave = new byte[32];
        for (int i = 0; i < clave.length; i++) {
            clave[i] = (byte) i;
        }
        return clave;
    }

    @BeforeEach
    void construir() {
        Catalogo catalogo = new Catalogo(List.of(new Candidato(new CandidateId("0b1c2d3e-4f50-4a61-8b72-93a4b5c6d7e8"),
                "A", "B")), List.of(new Region("norte"), new Region("sur")));
        servicio = new ServicioPadron(DERIVADOR, repositorio, publicador, transaccion, () -> catalogo, ELECCION,
                Clock.fixed(AHORA, ZoneOffset.UTC));
    }

    @SuppressWarnings("unchecked")
    private void transaccionEjecutaLaAccion() {
        when(transaccion.ejecutar(any(Supplier.class))).thenAnswer(i -> ((Supplier<?>) i.getArgument(0)).get());
    }

    @Test
    @DisplayName("Debe inscribir y publicar el UUID derivado de la cédula, el mismo que da POST /voter-id, sin la cédula")
    void inscribe() {
        // Arrange
        transaccionEjecutaLaAccion();
        ArgumentCaptor<VotanteInscrito> inscrito = ArgumentCaptor.forClass(VotanteInscrito.class);

        // Act
        servicio.inscribir("0.012.345.678", "  Ana Pérez  ", "norte");

        // Assert
        verify(repositorio).inscribir(inscrito.capture());
        verify(publicador).publicar(inscrito.getValue());
        VotanteInscrito votante = inscrito.getValue();
        assertThat(votante.voterId()).isEqualTo(new VoterId("fae9cb04-ba2b-87ef-882c-f39dd70dbaa1"));
        assertThat(votante.voterId()).isEqualTo(DERIVADOR.derivar(new Cedula("0012345678"), ELECCION));
        assertThat(votante.nombre().valor()).isEqualTo("Ana Pérez");
        assertThat(votante.region()).isEqualTo(new Region("norte"));
        assertThat(votante.inscritoEn()).isEqualTo(AHORA);
        assertThat(votante.toString()).doesNotContain("0012345678");
    }

    @Test
    @DisplayName("Debe inscribir antes de publicar, dentro de la transacción")
    void orden() {
        transaccionEjecutaLaAccion();

        servicio.inscribir("1020304051", "Ana", "sur");

        InOrder orden = inOrder(transaccion, repositorio, publicador);
        orden.verify(transaccion).ejecutar(any());
        orden.verify(repositorio).inscribir(any());
        orden.verify(publicador).publicar(any());
    }

    @Test
    @DisplayName("Debe reunir los campos inválidos, incluida una región fuera del catálogo, sin tocar BD ni Kafka")
    void invalidos() {
        SolicitudVotoInvalidaException todos = assertThrows(SolicitudVotoInvalidaException.class,
                () -> servicio.inscribir("12ab", " ", "centro"));
        SolicitudVotoInvalidaException ausentes = assertThrows(SolicitudVotoInvalidaException.class,
                () -> servicio.inscribir(null, null, null));

        assertThat(todos.campos()).containsExactly("cedula", "fullName", "region");
        assertThat(ausentes.campos()).containsExactly("cedula", "fullName", "region");
        verifyNoInteractions(repositorio, publicador, transaccion);
    }

    @Test
    @DisplayName("Si el documento ya está inscrito, no debe publicar")
    void yaInscrito() {
        transaccionEjecutaLaAccion();
        doThrow(new VotanteYaInscritoException()).when(repositorio).inscribir(any());

        assertThrows(VotanteYaInscritoException.class, () -> servicio.inscribir("1020304051", "Ana", "norte"));

        verify(publicador, never()).publicar(any());
    }

    @Test
    @DisplayName("Si Kafka no confirma, el error sale de la transacción para que se revierta la inscripción")
    void publicacionFalla() {
        transaccionEjecutaLaAccion();
        doThrow(new PublicacionNoDisponibleException()).when(publicador).publicar(any());

        assertThrows(PublicacionNoDisponibleException.class, () -> servicio.inscribir("1020304051", "Ana", "norte"));

        verify(repositorio).inscribir(any());
    }
}

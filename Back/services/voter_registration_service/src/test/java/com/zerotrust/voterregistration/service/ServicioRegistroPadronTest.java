package com.zerotrust.voterregistration.service;

import com.zerotrust.voterregistration.domain.PadronInvalidoException;
import com.zerotrust.voterregistration.domain.RegistroVotanteCrudo;
import com.zerotrust.voterregistration.domain.VotanteRegistrado;
import com.zerotrust.voterregistration.domain.VoterId;
import com.zerotrust.voterregistration.port.FuentePadron;
import com.zerotrust.voterregistration.port.PublicadorElegibles;
import com.zerotrust.voterregistration.port.RepositorioPadron;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ServicioRegistroPadronTest {

    private static final String ID_1 = "3f2b8c1e-6d4a-4b7e-9c21-0a5d6e7f8a9b";
    private static final String ID_2 = "9a8b7c6d-5e4f-4a3b-8c2d-1e0f2a3b4c5d";
    private static final Instant AHORA = Instant.parse("2026-11-14T08:00:00Z");

    @Mock
    private FuentePadron fuente;
    @Mock
    private RepositorioPadron repositorio;
    @Mock
    private PublicadorElegibles publicador;

    private ServicioRegistroPadron servicio;

    @BeforeEach
    void construir() {
        servicio = new ServicioRegistroPadron(fuente, repositorio, publicador, Clock.fixed(AHORA, ZoneOffset.UTC));
    }

    @Test
    @DisplayName("Debe guardar el padrón una vez y después publicar cada votante")
    void registra() {
        // Arrange
        when(fuente.leer()).thenReturn(List.of(new RegistroVotanteCrudo(ID_1, "norte"),
                new RegistroVotanteCrudo(ID_2, "sur")));
        when(repositorio.guardarNuevos(anyList())).thenReturn(1);
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<VotanteRegistrado>> guardados = ArgumentCaptor.forClass(List.class);

        // Act
        ResultadoRegistro resultado = servicio.registrar();

        // Assert
        verify(repositorio, times(1)).guardarNuevos(guardados.capture());
        assertThat(guardados.getValue()).hasSize(2);
        verify(publicador, times(2)).publicar(any());
        assertThat(resultado.total()).isEqualTo(2);
        assertThat(resultado.nuevos()).isEqualTo(1);
        InOrder orden = inOrder(repositorio, publicador);
        orden.verify(repositorio).guardarNuevos(anyList());
        orden.verify(publicador, times(2)).publicar(any());
    }

    @Test
    @DisplayName("Cada votante publicado lleva el instante de esta ejecución")
    void publicaConInstante() {
        when(fuente.leer()).thenReturn(List.of(new RegistroVotanteCrudo(ID_1, "norte")));
        ArgumentCaptor<VotanteRegistrado> publicado = ArgumentCaptor.forClass(VotanteRegistrado.class);

        servicio.registrar();

        verify(publicador).publicar(publicado.capture());
        assertThat(publicado.getValue().voterId()).isEqualTo(new VoterId(ID_1));
        assertThat(publicado.getValue().registradoEn()).isEqualTo(AHORA);
    }

    @Test
    @DisplayName("Con un registro inválido no guarda ni publica nada")
    void registroInvalido() {
        when(fuente.leer()).thenReturn(List.of(new RegistroVotanteCrudo(ID_1, "norte"),
                new RegistroVotanteCrudo("x", "sur")));

        assertThrows(PadronInvalidoException.class, () -> servicio.registrar());

        verifyNoInteractions(repositorio, publicador);
    }

    @Test
    @DisplayName("Si la base de datos falla no publica nada")
    void falloBaseDeDatos() {
        when(fuente.leer()).thenReturn(List.of(new RegistroVotanteCrudo(ID_1, "norte")));
        when(repositorio.guardarNuevos(anyList())).thenThrow(new IllegalStateException("sin base de datos"));

        assertThrows(IllegalStateException.class, () -> servicio.registrar());

        verify(publicador, never()).publicar(any());
    }

    @Test
    @DisplayName("Si Kafka falla el error se propaga para terminar con código distinto de 0")
    void falloKafka() {
        when(fuente.leer()).thenReturn(List.of(new RegistroVotanteCrudo(ID_1, "norte")));
        doThrow(new IllegalStateException("sin broker")).when(publicador).publicar(any());

        assertThrows(IllegalStateException.class, () -> servicio.registrar());
    }
}

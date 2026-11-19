package com.zerotrust.votingapi.service;

import com.zerotrust.votingapi.domain.Cedula;
import com.zerotrust.votingapi.domain.ElectionId;
import com.zerotrust.votingapi.exception.ValidacionNoDisponibleException;
import com.zerotrust.votingapi.exception.VotanteFueraDelPadronException;
import com.zerotrust.votingapi.domain.VoterId;
import com.zerotrust.votingapi.port.DerivadorVoterId;
import com.zerotrust.votingapi.port.ValidadorVotante;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ServicioIdentificacionTest {

    private static final ElectionId ELECCION = new ElectionId("elecciones-2026");
    private static final Cedula CEDULA = new Cedula("0012345678");
    private static final VoterId VOTER = new VoterId("fae9cb04-ba2b-87ef-882c-f39dd70dbaa1");

    @Mock
    private DerivadorVoterId derivador;
    @Mock
    private ValidadorVotante validador;

    private ServicioIdentificacion servicio;

    @BeforeEach
    void construir() {
        servicio = new ServicioIdentificacion(derivador, validador, ELECCION);
        when(derivador.derivar(CEDULA, ELECCION)).thenReturn(VOTER);
    }

    @Test
    @DisplayName("Si el titular está en el padrón, debe devolver el voterId de la elección en curso")
    void inscrito() {
        // Arrange
        when(validador.estaHabilitado(VOTER)).thenReturn(true);

        // Act
        VoterId voterId = servicio.identificar(CEDULA);

        // Assert
        assertThat(voterId).isEqualTo(VOTER);
        verify(validador).estaHabilitado(VOTER);
    }

    @Test
    @DisplayName("Si el titular no está en el padrón, debe avisarlo en lugar de entregar el código")
    void fueraDelPadron() {
        when(validador.estaHabilitado(VOTER)).thenReturn(false);

        assertThrows(VotanteFueraDelPadronException.class, () -> servicio.identificar(CEDULA));
    }

    @Test
    @DisplayName("Si la validación no responde, el error se propaga (503) y no se entrega el código")
    void noDisponible() {
        when(validador.estaHabilitado(VOTER)).thenThrow(new ValidacionNoDisponibleException());

        assertThrows(ValidacionNoDisponibleException.class, () -> servicio.identificar(CEDULA));
    }
}

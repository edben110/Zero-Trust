package com.zerotrust.votingapi.service;

import com.zerotrust.votingapi.domain.Cedula;
import com.zerotrust.votingapi.domain.ElectionId;
import com.zerotrust.votingapi.domain.VoterId;
import com.zerotrust.votingapi.port.DerivadorVoterId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ServicioIdentificacionTest {

    private static final ElectionId ELECCION = new ElectionId("elecciones-2026");

    @Mock
    private DerivadorVoterId derivador;

    @Test
    @DisplayName("Debe derivar el voterId con la elección en curso, una sola vez")
    void identifica() {
        // Arrange
        Cedula cedula = new Cedula("0012345678");
        VoterId esperado = new VoterId("fae9cb04-ba2b-87ef-882c-f39dd70dbaa1");
        when(derivador.derivar(cedula, ELECCION)).thenReturn(esperado);

        // Act
        VoterId voterId = new ServicioIdentificacion(derivador, ELECCION).identificar(cedula);

        // Assert
        assertThat(voterId).isEqualTo(esperado);
        verify(derivador).derivar(cedula, ELECCION);
        verifyNoMoreInteractions(derivador);
    }
}

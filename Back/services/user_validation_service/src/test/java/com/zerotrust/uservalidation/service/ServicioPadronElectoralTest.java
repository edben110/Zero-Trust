package com.zerotrust.uservalidation.service;

import com.zerotrust.uservalidation.domain.VoterId;
import com.zerotrust.uservalidation.port.PadronVotantes;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ServicioPadronElectoralTest {

    private static final VoterId VOTANTE = new VoterId("3f2b8c1e-6d4a-4b7e-9c21-0a5d6e7f8a9b");

    @Mock
    private PadronVotantes padron;

    @InjectMocks
    private ServicioPadronElectoral servicio;

    @Test
    @DisplayName("Registrar un votante elegible debe habilitarlo exactamente una vez")
    void registra() {
        servicio.registrarElegible(VOTANTE);

        verify(padron, times(1)).habilitar(VOTANTE);
        verifyNoMoreInteractions(padron);
    }

    @Test
    @DisplayName("Retirar un votante debe quitarlo del padrón exactamente una vez")
    void retira() {
        servicio.retirar(VOTANTE);

        verify(padron, times(1)).retirar(VOTANTE);
        verifyNoMoreInteractions(padron);
    }

    @Test
    @DisplayName("La validación debe responder según el padrón")
    void valida() {
        when(padron.estaHabilitado(VOTANTE)).thenReturn(true);

        assertThat(servicio.esElegible(VOTANTE)).isTrue();
        verify(padron, times(1)).estaHabilitado(VOTANTE);
    }

    @Test
    @DisplayName("Un votante que no está en el padrón no es elegible")
    void noElegible() {
        when(padron.estaHabilitado(VOTANTE)).thenReturn(false);

        assertThat(servicio.esElegible(VOTANTE)).isFalse();
    }
}

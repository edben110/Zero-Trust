package com.zerotrust.voterregistration.config;

import com.zerotrust.voterregistration.domain.PadronInvalidoException;
import com.zerotrust.voterregistration.service.ResultadoRegistro;
import com.zerotrust.voterregistration.service.ServicioRegistroPadron;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.DefaultApplicationArguments;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TareaRegistroPadronTest {

    @Mock
    private ServicioRegistroPadron servicio;

    @Test
    @DisplayName("Al arrancar debe registrar el padrón una sola vez")
    void ejecutaUnaVez() {
        when(servicio.registrar()).thenReturn(new ResultadoRegistro(8, 8));

        assertThatCode(() -> new TareaRegistroPadron(servicio).run(new DefaultApplicationArguments()))
                .doesNotThrowAnyException();

        verify(servicio, times(1)).registrar();
    }

    @Test
    @DisplayName("Un fallo debe propagarse para que el proceso termine con código distinto de 0")
    void propagaFallo() {
        when(servicio.registrar()).thenThrow(new PadronInvalidoException("registro [3]: region inválido"));

        assertThrows(PadronInvalidoException.class,
                () -> new TareaRegistroPadron(servicio).run(new DefaultApplicationArguments()));
    }

    @Test
    @DisplayName("El código de salida es 0 si la tarea termina y 1 si falla")
    void codigoDeSalida() {
        assertThatCode(() -> {
            if (CodigoSalida.de(() -> { }) != 0 || CodigoSalida.de(() -> { throw new IllegalStateException(); }) != 1) {
                throw new AssertionError("códigos de salida incorrectos");
            }
        }).doesNotThrowAnyException();
    }
}

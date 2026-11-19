package com.zerotrust.votingapi.service;

import com.zerotrust.votingapi.domain.CanalSeguridadNoDisponibleException;
import com.zerotrust.votingapi.domain.DireccionIp;
import com.zerotrust.votingapi.domain.EstadoIp;
import com.zerotrust.votingapi.domain.IncidenteSeguridad;
import com.zerotrust.votingapi.domain.OperacionSeguridadNoDisponibleException;
import com.zerotrust.votingapi.domain.PanelSeguridad;
import com.zerotrust.votingapi.domain.PruebaSeguridad;
import com.zerotrust.votingapi.domain.ResultadoPurga;
import com.zerotrust.votingapi.domain.ResumenSeguridad;
import com.zerotrust.votingapi.domain.UmbralesSeguridad;
import com.zerotrust.votingapi.port.ConsultaSeguridad;
import com.zerotrust.votingapi.port.PublicadorComandosSeguridad;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ServicioSeguridadAdminTest {

    private static final DireccionIp IP = new DireccionIp("203.0.113.10");
    private static final UmbralesSeguridad UMBRALES = new UmbralesSeguridad(5, 10, 60);

    @Mock
    private ConsultaSeguridad consulta;

    @Mock
    private PublicadorComandosSeguridad publicador;

    private ServicioSeguridadAdmin servicio;

    @BeforeEach
    void preparar() {
        servicio = new ServicioSeguridadAdmin(consulta, publicador, UMBRALES);
    }

    private static CanalSeguridadNoDisponibleException caido() {
        return new CanalSeguridadNoDisponibleException("caído", new RuntimeException());
    }

    @Nested
    @DisplayName("Simulacro")
    class Simulacro {

        @Test
        @DisplayName("Debe publicar un EVALUATE por cada voto simulado")
        void publicaUnaEvaluacionPorVoto() {
            // Act
            servicio.simular(new PruebaSeguridad(IP, 3));

            // Assert
            verify(publicador, times(3)).evaluar(IP);
            verifyNoInteractions(consulta);
        }

        @Test
        @DisplayName("Debe informar que no se publicó la prueba si Kafka no confirma")
        void kafkaNoDisponible() {
            // Arrange
            doThrow(caido()).when(publicador).evaluar(IP);

            // Act
            OperacionSeguridadNoDisponibleException error = assertThrows(OperacionSeguridadNoDisponibleException.class,
                    () -> servicio.simular(new PruebaSeguridad(IP, 3)));

            // Assert
            assertEquals(ServicioSeguridadAdmin.PRUEBA_NO_PUBLICADA, error.getMessage());
            verify(publicador, times(1)).evaluar(IP);
        }
    }

    @Nested
    @DisplayName("Reinicio de una IP")
    class Reinicio {

        @Test
        @DisplayName("Debe publicar RESET para la IP")
        void publicaReset() {
            // Act
            servicio.reiniciar(IP);

            // Assert
            verify(publicador).reiniciar(IP);
        }

        @Test
        @DisplayName("Debe informar que no se publicó el reinicio si Kafka no confirma")
        void kafkaNoDisponible() {
            // Arrange
            doThrow(caido()).when(publicador).reiniciar(IP);

            // Act
            OperacionSeguridadNoDisponibleException error = assertThrows(OperacionSeguridadNoDisponibleException.class,
                    () -> servicio.reiniciar(IP));

            // Assert
            assertEquals(ServicioSeguridadAdmin.REINICIO_NO_PUBLICADO, error.getMessage());
        }
    }

    @Nested
    @DisplayName("Purga")
    class Purga {

        @Test
        @DisplayName("Debe reiniciar el detector antes de borrar PostgreSQL")
        void reiniciaYLuegoBorra() {
            // Arrange
            when(consulta.purgar()).thenReturn(new ResultadoPurga(4, 2));

            // Act
            ResultadoPurga resultado = servicio.purgar();

            // Assert
            InOrder orden = inOrder(publicador, consulta);
            orden.verify(publicador).reiniciarTodo();
            orden.verify(consulta).purgar();
            assertEquals(new ResultadoPurga(4, 2), resultado);
        }

        @Test
        @DisplayName("No debe borrar nada si Kafka no confirma el reinicio del detector")
        void sinKafkaNoBorra() {
            // Arrange
            doThrow(caido()).when(publicador).reiniciarTodo();

            // Act
            OperacionSeguridadNoDisponibleException error = assertThrows(OperacionSeguridadNoDisponibleException.class,
                    () -> servicio.purgar());

            // Assert
            assertEquals(ServicioSeguridadAdmin.DETECTOR_NO_REINICIADO, error.getMessage());
            verify(consulta, never()).purgar();
        }

        @Test
        @DisplayName("Debe avisar que el detector se reinició pero PostgreSQL no se purgó")
        void baseDeDatosNoDisponible() {
            // Arrange
            when(consulta.purgar()).thenThrow(caido());

            // Act
            OperacionSeguridadNoDisponibleException error = assertThrows(OperacionSeguridadNoDisponibleException.class,
                    () -> servicio.purgar());

            // Assert
            assertEquals(ServicioSeguridadAdmin.REGISTROS_NO_PURGADOS, error.getMessage());
        }
    }

    @Nested
    @DisplayName("Panel")
    class Panel {

        @Test
        @DisplayName("Debe componer umbrales, resumen, estados e incidentes con sus límites")
        void componeElPanel() {
            // Arrange
            ResumenSeguridad resumen = new ResumenSeguridad(1, 1, 0, 0, 0, 1, Map.of("NORMAL", 1L));
            List<EstadoIp> estados = List.of(new EstadoIp("1.1.1.1", "NORMAL", 1, 60, Instant.EPOCH, Instant.EPOCH));
            List<IncidenteSeguridad> incidentes = List.of(
                    new IncidenteSeguridad(1, "1.1.1.1", "IP_PATTERN", "SOSPECHOSA", 6, 60, "LIVE", Instant.EPOCH));
            when(consulta.resumen()).thenReturn(resumen);
            when(consulta.estados(ServicioSeguridadAdmin.MAX_ESTADOS)).thenReturn(estados);
            when(consulta.incidentesRecientes(ServicioSeguridadAdmin.MAX_INCIDENTES)).thenReturn(incidentes);

            // Act
            PanelSeguridad panel = servicio.panel();

            // Assert
            assertEquals(new PanelSeguridad(UMBRALES, resumen, estados, incidentes), panel);
        }

        @Test
        @DisplayName("Debe informar que no se pudo consultar PostgreSQL")
        void baseDeDatosNoDisponible() {
            // Arrange
            when(consulta.resumen()).thenThrow(caido());

            // Act
            OperacionSeguridadNoDisponibleException error = assertThrows(OperacionSeguridadNoDisponibleException.class,
                    () -> servicio.panel());

            // Assert
            assertEquals(ServicioSeguridadAdmin.CONSULTA_NO_DISPONIBLE, error.getMessage());
        }
    }

    @Test
    @DisplayName("Debe exponer el destino de los comandos para la respuesta del panel")
    void exponeElDestino() {
        // Arrange
        when(publicador.destino()).thenReturn("security_drills");

        // Act
        String destino = servicio.destinoComandos();

        // Assert
        assertEquals("security_drills", destino);
    }
}

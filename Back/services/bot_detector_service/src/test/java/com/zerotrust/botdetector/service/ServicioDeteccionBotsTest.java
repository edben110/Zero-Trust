package com.zerotrust.botdetector.service;

import com.zerotrust.botdetector.domain.AlertOrigin;
import com.zerotrust.botdetector.domain.SecurityAlert;
import com.zerotrust.botdetector.exception.ProyeccionNoDisponibleException;
import com.zerotrust.botdetector.port.FraudIncidentRepository;
import com.zerotrust.botdetector.port.RiskStateRepository;
import com.zerotrust.botdetector.port.SecurityAlertPublisher;
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

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * Cuando el estado de la IP genera alerta, el servicio la publica y la persiste exactamente una vez.
 */
@ExtendWith(MockitoExtension.class)
class ServicioDeteccionBotsTest {

    private static final Instant AHORA = Instant.parse("2026-11-18T12:00:00Z");
    private static final Clock RELOJ = Clock.fixed(AHORA, ZoneOffset.UTC);

    @Mock
    private SecurityAlertPublisher alertPublisher;

    @Mock
    private FraudIncidentRepository repository;

    @Mock
    private RiskStateRepository riskStates;

    // Umbrales pequeños para provocar alertas rápido en los tests.
    private final RegistroContextosIp registro = new RegistroContextosIp(2, 4, 60);

    private ServicioDeteccionBots svc;

    @BeforeEach
    void preparar() {
        svc = new ServicioDeteccionBots(alertPublisher, repository, riskStates, registro, RELOJ);
    }

    @Test
    @DisplayName("No debe alertar bajo el umbral, pero sí proyectar cada voto")
    void noPublicaAlertaBajoUmbral() {
        // Act: 2 votos, umbral sospechosa = 2, todavía no se supera
        svc.evaluate("1.1.1.1");
        svc.evaluate("1.1.1.1");

        // Assert
        verify(alertPublisher, never()).publish(any());
        verify(repository, never()).save(any());
        verify(riskStates, times(2)).actualizar(eq("1.1.1.1"), eq("NORMAL"), anyInt(), eq(60), eq(AHORA));
        verify(riskStates).actualizar("1.1.1.1", "NORMAL", 2, 60, AHORA);
    }

    @Test
    @DisplayName("Debe publicar y persistir la alerta al superar el umbral")
    void publicaAlertaYPersisteCuandoSuperaUmbral() {
        // Arrange
        svc.evaluate("2.2.2.2");
        svc.evaluate("2.2.2.2");

        // Act: 3er voto supera umbral sospechosa (2)
        svc.evaluate("2.2.2.2");

        // Assert
        ArgumentCaptor<SecurityAlert> alerta = ArgumentCaptor.forClass(SecurityAlert.class);
        verify(alertPublisher, times(1)).publish(alerta.capture());
        verify(repository, times(1)).save(any(SecurityAlert.class));
        assertEquals("SOSPECHOSA", alerta.getValue().riskLevel());
        assertEquals(AlertOrigin.LIVE, alerta.getValue().origin());
        verify(riskStates).actualizar("2.2.2.2", "SOSPECHOSA", 3, 60, AHORA);
    }

    @Test
    @DisplayName("Una prueba del panel debe registrarse con origen DRILL")
    void unaPruebaDelPanelSeRegistraConOrigenDrill() {
        // Act: simulación controlada de 3 votos desde la misma IP
        svc.evaluate("203.0.113.10", AlertOrigin.DRILL);
        svc.evaluate("203.0.113.10", AlertOrigin.DRILL);
        svc.evaluate("203.0.113.10", AlertOrigin.DRILL);

        // Assert
        ArgumentCaptor<SecurityAlert> alerta = ArgumentCaptor.forClass(SecurityAlert.class);
        verify(alertPublisher).publish(alerta.capture());
        verify(repository).save(any(SecurityAlert.class));
        assertEquals(AlertOrigin.DRILL, alerta.getValue().origin());
        assertEquals("203.0.113.10", alerta.getValue().ip());
    }

    @Test
    @DisplayName("IPs diferentes no deben interferir entre ellas")
    void ipsDiferentesNoSeInterfierenEntreEllas() {
        // Act: IP-A supera umbral, IP-B no
        svc.evaluate("3.3.3.3");
        svc.evaluate("3.3.3.3");
        svc.evaluate("3.3.3.3");
        svc.evaluate("4.4.4.4");

        // Assert
        verify(alertPublisher, times(1)).publish(any());
        assertEquals("NORMAL", registro.nivelRiesgo("4.4.4.4"));
    }

    @Test
    @DisplayName("Debe publicar y guardar la alerta antes de proyectar el estado de la IP")
    void publicaLaAlertaAntesDeProyectar() {
        // Arrange
        svc.evaluate("8.8.8.8");
        svc.evaluate("8.8.8.8");
        clearInvocations(riskStates);

        // Act
        svc.evaluate("8.8.8.8");

        // Assert
        InOrder orden = inOrder(alertPublisher, repository, riskStates);
        orden.verify(alertPublisher).publish(any(SecurityAlert.class));
        orden.verify(repository).save(any(SecurityAlert.class));
        orden.verify(riskStates).actualizar("8.8.8.8", "SOSPECHOSA", 3, 60, AHORA);
    }

    @Test
    @DisplayName("Debe publicar la alerta aunque la proyección del estado en PostgreSQL falle")
    void publicaLaAlertaAunqueLaProyeccionFalle() {
        // Arrange
        doThrow(new ProyeccionNoDisponibleException("BD caída", new RuntimeException()))
                .when(riskStates).actualizar(anyString(), anyString(), anyInt(), anyInt(), any());

        // Act
        assertDoesNotThrow(() -> {
            svc.evaluate("9.9.9.9");
            svc.evaluate("9.9.9.9");
            svc.evaluate("9.9.9.9");
        });

        // Assert
        verify(alertPublisher, times(1)).publish(any(SecurityAlert.class));
        verify(repository, times(1)).save(any(SecurityAlert.class));
        assertEquals("SOSPECHOSA", registro.nivelRiesgo("9.9.9.9"));
    }
}

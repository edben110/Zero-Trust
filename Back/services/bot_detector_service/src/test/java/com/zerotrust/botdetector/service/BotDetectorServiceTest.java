package com.zerotrust.botdetector.service;

import com.zerotrust.botdetector.domain.model.AlertOrigin;
import com.zerotrust.botdetector.domain.model.SecurityAlert;
import com.zerotrust.botdetector.domain.port.FraudIncidentRepository;
import com.zerotrust.botdetector.domain.port.RiskStateRepository;
import com.zerotrust.botdetector.domain.port.SecurityAlertPublisher;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Prueba de orquestación. Verifica que cuando el estado de la IP genera alerta,
 * el servicio llama exactamente una vez al publisher Y al repositorio.
 */
@ExtendWith(MockitoExtension.class)
class BotDetectorServiceTest {

    private static final Instant AHORA = Instant.parse("2026-11-18T12:00:00Z");
    private static final Clock RELOJ = Clock.fixed(AHORA, ZoneOffset.UTC);

    @Mock
    private SecurityAlertPublisher  alertPublisher;

    @Mock
    private FraudIncidentRepository repository;

    @Mock
    private RiskStateRepository     riskStates;

    // Umbrales pequeños para provocar alertas rápido en los tests.
    private BotDetectorService buildService() {
        return new BotDetectorService(alertPublisher, repository, riskStates, 2, 4, 60, RELOJ);
    }

    @Test
    void noPublicaAlertaBajoUmbral() {
        // Arrange
        BotDetectorService svc = buildService();

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
    void publicaAlertaYPersisteCuandoSuperaUmbral() {
        // Arrange
        BotDetectorService svc = buildService();
        svc.evaluate("2.2.2.2");
        svc.evaluate("2.2.2.2");

        // Act: 3er voto supera umbral sospechosa (2)
        svc.evaluate("2.2.2.2");

        // Assert: alerta publicada Y persistida exactamente una vez
        ArgumentCaptor<SecurityAlert> alerta = ArgumentCaptor.forClass(SecurityAlert.class);
        verify(alertPublisher, times(1)).publish(alerta.capture());
        verify(repository,     times(1)).save(any(SecurityAlert.class));
        assertEquals("SOSPECHOSA", alerta.getValue().riskLevel());
        assertEquals(AlertOrigin.LIVE, alerta.getValue().origin());
        verify(riskStates).actualizar("2.2.2.2", "SOSPECHOSA", 3, 60, AHORA);
    }

    @Test
    void unaPruebaDelPanelSeRegistraConOrigenDrill() {
        // Arrange
        BotDetectorService svc = buildService();

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
    void ipsDiferentesNoSeInterfierenEntreEllas() {
        // Arrange
        BotDetectorService svc = buildService();

        // Act: IP-A supera umbral, IP-B no
        svc.evaluate("3.3.3.3");
        svc.evaluate("3.3.3.3");
        svc.evaluate("3.3.3.3"); // supera umbral

        svc.evaluate("4.4.4.4"); // solo 1 voto

        // Assert: solo IP-A generó alerta
        verify(alertPublisher, times(1)).publish(any());
        assertEquals("NORMAL", svc.riskLevelOf("4.4.4.4"));
    }

    @Test
    void resetDevuelveLaIpBloqueadaANormal() {
        // Arrange: 5 votos con umbral bloqueada 4 → BLOQUEADA
        BotDetectorService svc = buildService();
        for (int i = 0; i < 5; i++) svc.evaluate("5.5.5.5");
        assertEquals("BLOQUEADA", svc.riskLevelOf("5.5.5.5"));

        // Act
        svc.reset("5.5.5.5");

        // Assert
        assertEquals("NORMAL", svc.riskLevelOf("5.5.5.5"));
        assertEquals(0, svc.voteCountOf("5.5.5.5"));
        verify(riskStates).actualizar("5.5.5.5", "NORMAL", 0, 60, AHORA);
    }
}

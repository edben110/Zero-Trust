package com.zerotrust.botdetector.service;

import com.zerotrust.botdetector.domain.AlertOrigin;
import com.zerotrust.botdetector.domain.FraudKind;
import com.zerotrust.botdetector.domain.SecurityAlert;
import com.zerotrust.botdetector.port.FraudIncidentRepository;
import com.zerotrust.botdetector.port.SecurityAlertPublisher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class ServicioRegistroFraudeTest {

    private static final Instant AHORA = Instant.parse("2026-11-18T12:00:00Z");
    private static final Clock RELOJ = Clock.fixed(AHORA, ZoneOffset.UTC);

    @Mock
    private SecurityAlertPublisher alertPublisher;

    @Mock
    private FraudIncidentRepository repository;

    private final RegistroContextosIp registro = new RegistroContextosIp(2, 4, 60);

    private ServicioRegistroFraude svc;

    @BeforeEach
    void preparar() {
        svc = new ServicioRegistroFraude(alertPublisher, repository, registro, RELOJ);
    }

    @Test
    @DisplayName("Debe publicar y persistir el incidente con su tipo sin alterar el estado de la IP")
    void registraUnIncidenteDeFraudeSinAlterarElEstadoDeLaIp() {
        // Arrange
        ArgumentCaptor<SecurityAlert> alerta = ArgumentCaptor.forClass(SecurityAlert.class);

        // Act: voto duplicado reportado por voting_api
        svc.recordFraud(FraudKind.DUPLICATE_VOTE, "6.6.6.6");

        // Assert
        verify(alertPublisher).publish(alerta.capture());
        verify(repository).save(alerta.getValue());
        assertEquals(FraudKind.DUPLICATE_VOTE, alerta.getValue().kind());
        assertEquals("NORMAL", alerta.getValue().riskLevel());
        assertEquals(AlertOrigin.LIVE, alerta.getValue().origin());
        assertEquals(60, alerta.getValue().windowSeconds());
        assertEquals("NORMAL", registro.nivelRiesgo("6.6.6.6"));
    }

    @Test
    @DisplayName("Debe reflejar en la alerta el nivel y los votos vigentes de la IP")
    void usaElEstadoVigente() {
        // Arrange
        for (int i = 0; i < 3; i++) {
            registro.contextoDe("7.7.7.7", AHORA).registerVote(AHORA);
        }
        ArgumentCaptor<SecurityAlert> alerta = ArgumentCaptor.forClass(SecurityAlert.class);

        // Act
        svc.recordFraud(FraudKind.WRONG_REGION, "7.7.7.7");

        // Assert
        verify(alertPublisher).publish(alerta.capture());
        assertEquals("SOSPECHOSA", alerta.getValue().riskLevel());
        assertEquals(3, alerta.getValue().voteCount());
    }

    @Test
    @DisplayName("Debe ignorar un incidente sin tipo o sin IP")
    void ignoraIncidenteIncompleto() {
        // Act
        svc.recordFraud(null, "6.6.6.6");
        svc.recordFraud(FraudKind.DUPLICATE_VOTE, " ");
        svc.recordFraud(FraudKind.DUPLICATE_VOTE, null);

        // Assert
        verifyNoInteractions(alertPublisher, repository);
    }
}

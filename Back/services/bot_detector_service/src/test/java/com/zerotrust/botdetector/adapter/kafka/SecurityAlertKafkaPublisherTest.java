package com.zerotrust.botdetector.adapter.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zerotrust.botdetector.domain.model.AlertOrigin;
import com.zerotrust.botdetector.domain.model.SecurityAlert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

import java.time.Instant;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SecurityAlertKafkaPublisherTest {

    private static final String TOPIC = "security_alerts";
    private static final String IP = "1.2.3.4";

    @Mock
    private KafkaTemplate<String, String> kafka;

    private SecurityAlertKafkaPublisher publicador;

    private final SecurityAlert alerta = new SecurityAlert(IP, "SOSPECHOSA", 6, 60, AlertOrigin.LIVE,
            Instant.parse("2026-11-18T12:00:00Z"));

    @BeforeEach
    void setUp() {
        publicador = new SecurityAlertKafkaPublisher(kafka, new ObjectMapper(), TOPIC, 3, 100, 0);
    }

    @SuppressWarnings("unchecked")
    private static CompletableFuture<SendResult<String, String>> confirmado() {
        return CompletableFuture.completedFuture(mock(SendResult.class));
    }

    private static CompletableFuture<SendResult<String, String>> fallido() {
        return CompletableFuture.failedFuture(new IllegalStateException("broker caído"));
    }

    @Test
    @DisplayName("Debe publicar la alerta con la IP como clave y esperar la confirmación")
    void publicaALaPrimera() {
        // Arrange
        ArgumentCaptor<String> json = ArgumentCaptor.forClass(String.class);
        when(kafka.send(eq(TOPIC), eq(IP), json.capture())).thenReturn(confirmado());

        // Act
        publicador.publish(alerta);

        // Assert
        verify(kafka, times(1)).send(eq(TOPIC), eq(IP), anyString());
        assertTrue(json.getValue().contains("\"riskLevel\":\"SOSPECHOSA\""));
    }

    @Test
    @DisplayName("Debe reintentar hasta que Kafka confirme el envío")
    void reintentaHastaConfirmar() {
        // Arrange
        when(kafka.send(eq(TOPIC), eq(IP), anyString())).thenReturn(fallido(), fallido(), confirmado());

        // Act
        publicador.publish(alerta);

        // Assert
        verify(kafka, times(3)).send(eq(TOPIC), eq(IP), anyString());
    }

    @Test
    @DisplayName("No debe relanzar tras agotar los intentos: corre dentro del consumidor y el voto se contaría dos veces")
    void noRelanzaTrasAgotarIntentos() {
        // Arrange
        when(kafka.send(eq(TOPIC), eq(IP), anyString())).thenReturn(fallido(), fallido(), fallido());

        // Act + Assert
        assertDoesNotThrow(() -> publicador.publish(alerta));
        verify(kafka, times(3)).send(eq(TOPIC), eq(IP), anyString());
    }

    @Test
    @DisplayName("Debe tratar como fallo un envío que Kafka no confirma a tiempo")
    void tratasComoFalloElTiempoAgotado() {
        // Arrange
        when(kafka.send(eq(TOPIC), eq(IP), anyString())).thenReturn(new CompletableFuture<>());

        // Act + Assert
        assertDoesNotThrow(() -> publicador.publish(alerta));
        verify(kafka, times(3)).send(eq(TOPIC), eq(IP), anyString());
    }
}

package com.zerotrust.regionalrollup.adapter.rabbit;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zerotrust.regionalrollup.domain.exception.PublicacionFallida;
import com.zerotrust.regionalrollup.domain.model.RegionalCandidateResult;
import com.zerotrust.regionalrollup.domain.model.RegionalResultSnapshot;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.AmqpConnectException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.net.ConnectException;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class RegionalResultRabbitPublisherTest {

    private static final String EXCHANGE = "live_results_regional";
    private static final String CLAVE = "results.norte";

    @Mock
    private RabbitTemplate rabbit;

    private RegionalResultRabbitPublisher publicador;

    private final RegionalResultSnapshot snapshot = new RegionalResultSnapshot(
            "norte", Instant.parse("2026-11-18T12:00:00Z"), List.of(new RegionalCandidateResult("c1", "c1", 2)));

    @BeforeEach
    void setUp() {
        publicador = new RegionalResultRabbitPublisher(rabbit, new ObjectMapper(), EXCHANGE, 3, 0);
    }

    private static AmqpConnectException caido() {
        return new AmqpConnectException(new ConnectException("broker caído"));
    }

    @Test
    @DisplayName("Debe publicar el snapshot con la clave results.<region> si el broker responde")
    void publicaALaPrimera() {
        // Arrange
        ArgumentCaptor<String> json = ArgumentCaptor.forClass(String.class);

        // Act
        publicador.publish(snapshot);

        // Assert
        verify(rabbit, times(1)).convertAndSend(eq(EXCHANGE), eq(CLAVE), json.capture());
        assertTrue(json.getValue().contains("\"region\":\"norte\""));
    }

    @Test
    @DisplayName("Debe reintentar y publicar cuando el broker se recupera antes del último intento")
    void reintentaHastaPublicar() {
        // Arrange
        doThrow(caido()).doThrow(caido()).doNothing()
                .when(rabbit).convertAndSend(eq(EXCHANGE), eq(CLAVE), anyString());

        // Act
        publicador.publish(snapshot);

        // Assert
        verify(rabbit, times(3)).convertAndSend(eq(EXCHANGE), eq(CLAVE), anyString());
    }

    @Test
    @DisplayName("Debe lanzar PublicacionFallida tras agotar los intentos")
    void lanzaTrasAgotarIntentos() {
        // Arrange
        doThrow(caido()).when(rabbit).convertAndSend(eq(EXCHANGE), eq(CLAVE), anyString());

        // Act + Assert
        assertThrows(PublicacionFallida.class, () -> publicador.publish(snapshot));
        verify(rabbit, times(3)).convertAndSend(eq(EXCHANGE), eq(CLAVE), anyString());
    }
}

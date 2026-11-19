package com.zerotrust.voteprocessor.adapter.rabbit;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zerotrust.voteprocessor.exception.PublicacionFallidaException;
import com.zerotrust.voteprocessor.domain.model.CandidateId;
import com.zerotrust.voteprocessor.domain.model.CandidateResult;
import com.zerotrust.voteprocessor.domain.model.GlobalResultSnapshot;
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
class GlobalResultRabbitPublisherTest {

    private static final String EXCHANGE = "live_results_global";
    private static final String C1 = "11111111-1111-1111-1111-111111111111";

    @Mock
    private RabbitTemplate rabbit;

    private GlobalResultRabbitPublisher publicador;

    private final GlobalResultSnapshot snapshot = new GlobalResultSnapshot(
            Instant.parse("2026-11-18T12:00:00Z"), List.of(new CandidateResult(new CandidateId(C1), C1, 4)));

    @BeforeEach
    void setUp() {
        publicador = new GlobalResultRabbitPublisher(rabbit, new ObjectMapper(), EXCHANGE, 3, 0);
    }

    private static AmqpConnectException caido() {
        return new AmqpConnectException(new ConnectException("broker caído"));
    }

    @Test
    @DisplayName("Debe publicar el snapshot en el fanout una sola vez si el broker responde")
    void publicaALaPrimera() {
        // Arrange
        ArgumentCaptor<String> json = ArgumentCaptor.forClass(String.class);

        // Act
        publicador.publish(snapshot);

        // Assert
        verify(rabbit, times(1)).convertAndSend(eq(EXCHANGE), eq(""), json.capture());
        assertTrue(json.getValue().contains("\"voteCount\":4"));
    }

    @Test
    @DisplayName("Debe reintentar y publicar cuando el broker se recupera antes del último intento")
    void reintentaHastaPublicar() {
        // Arrange
        doThrow(caido()).doThrow(caido()).doNothing()
                .when(rabbit).convertAndSend(eq(EXCHANGE), eq(""), anyString());

        // Act
        publicador.publish(snapshot);

        // Assert
        verify(rabbit, times(3)).convertAndSend(eq(EXCHANGE), eq(""), anyString());
    }

    @Test
    @DisplayName("Debe lanzar PublicacionFallidaException tras agotar los intentos")
    void lanzaTrasAgotarIntentos() {
        // Arrange
        doThrow(caido()).when(rabbit).convertAndSend(eq(EXCHANGE), eq(""), anyString());

        // Act + Assert
        assertThrows(PublicacionFallidaException.class, () -> publicador.publish(snapshot));
        verify(rabbit, times(3)).convertAndSend(eq(EXCHANGE), eq(""), anyString());
    }
}

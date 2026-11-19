package com.zerotrust.regionaldashboardworker.messaging.rabbit;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RegionalResultsRabbitConsumerTest {

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @Spy
    private ObjectMapper mapper = new ObjectMapper();

    @InjectMocks
    private RegionalResultsRabbitConsumer consumer;

    @Test
    void reenvíaMensajeATopicDeLaRegionCorrecta() throws Exception {
        // Arrange
        String message = "{\"region\":\"norte\",\"timestamp\":\"2026-01-01T00:00:00Z\",\"results\":[]}";

        // Act
        consumer.receive(message);

        // Assert
        verify(messagingTemplate, times(1)).convertAndSend("/topic/region/norte", message);
    }

    @Test
    void mensajeSinRegionNoReenvía() {
        // Arrange
        String message = "{\"timestamp\":\"2026-01-01T00:00:00Z\",\"results\":[]}";

        // Act
        consumer.receive(message);

        // Assert
        verify(messagingTemplate, never()).convertAndSend(anyString(), (Object) any());
    }

    @Test
    void mensajeMalformadoNoLanzaExcepcion() {
        // Act & Assert: no debe propagar la excepción
        consumer.receive("esto no es json válido {{{");
        verify(messagingTemplate, never()).convertAndSend(anyString(), (Object) any());
    }
}

package com.zerotrust.globaldashboardworker.adapter;

import com.zerotrust.globaldashboardworker.adapter.rabbit.GlobalResultsRabbitConsumer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class GlobalResultsRabbitConsumerTest {

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @InjectMocks
    private GlobalResultsRabbitConsumer consumer;

    @Test
    void reenvíaMensajeATopicGlobal() {
        // Arrange
        String payload = "{\"timestamp\":\"2026-01-01T00:00:00Z\",\"results\":[]}";

        // Act
        consumer.receive(payload);

        // Assert: el mensaje fue reenviado exactamente una vez al topic correcto
        verify(messagingTemplate, times(1)).convertAndSend("/topic/global", payload);
    }
}

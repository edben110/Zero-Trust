package com.zerotrust.globaldashboardworker.adapter.rabbit;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

/**
 * Escucha el Fanout Exchange live_results_global y reenvía cada mensaje
 * a todos los clientes WebSocket suscritos a /topic/global.
 *
 * El mensaje llega ya como JSON (producido por vote_processor) y se
 * reenvía directamente sin deserializar, para no duplicar el trabajo.
 */
@Component
public class GlobalResultsRabbitConsumer {

    private final SimpMessagingTemplate messagingTemplate;

    public GlobalResultsRabbitConsumer(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    @RabbitListener(queues = "#{globalDashboardQueue.name}")
    public void receive(String message) {
        messagingTemplate.convertAndSend("/topic/global", message);
    }
}

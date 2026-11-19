package com.zerotrust.globaldashboardworker.messaging.rabbit;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

// Reenvía el JSON tal cual llega de vote_processor, sin deserializarlo.
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

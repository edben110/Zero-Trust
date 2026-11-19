package com.zerotrust.regionaldashboardworker.messaging.rabbit;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Escucha el Topic Exchange live_results_regional y reenvía cada snapshot
 * a los clientes WebSocket suscritos a /topic/region/<nombre-de-región>.
 *
 * Extrae el campo "region" del payload para construir el destino dinámico,
 * de modo que cada cliente puede suscribirse solo a su región de interés.
 */
@Component
public class RegionalResultsRabbitConsumer {

    private static final Logger log = LoggerFactory.getLogger(RegionalResultsRabbitConsumer.class);

    private final SimpMessagingTemplate messagingTemplate;
    private final ObjectMapper          mapper;

    public RegionalResultsRabbitConsumer(SimpMessagingTemplate messagingTemplate,
                                         ObjectMapper mapper) {
        this.messagingTemplate = messagingTemplate;
        this.mapper            = mapper;
    }

    @RabbitListener(queues = "#{regionalDashboardQueue.name}")
    public void receive(String message) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> payload = mapper.readValue(message, Map.class);
            String region = (String) payload.get("region");

            if (region != null && !region.isBlank()) {
                messagingTemplate.convertAndSend("/topic/region/" + region, message);
            }
        } catch (Exception e) {
            log.warn("Resultado regional descartado: {}", e.getMessage());
        }
    }
}

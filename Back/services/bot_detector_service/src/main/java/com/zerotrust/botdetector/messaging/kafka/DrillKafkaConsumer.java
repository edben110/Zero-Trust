package com.zerotrust.botdetector.messaging.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zerotrust.botdetector.domain.AlertOrigin;
import com.zerotrust.botdetector.service.BotDetectorService;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Consume las pruebas controladas que lanza el panel de administración.
 *
 * No se inyectan votos en raw_votes para no contaminar el escrutinio ni la
 * auditoría electoral. El detector recibe la IP y ejecuta exactamente el mismo
 * patrón State que usa con el tráfico real; por eso el incidente queda con
 * origin=DRILL y es distinguible de un ataque real.
 */
@Component
public class DrillKafkaConsumer {

    private static final Logger log = LoggerFactory.getLogger(DrillKafkaConsumer.class);

    private static final int MAX_VOTOS_POR_MENSAJE = 50;

    private final BotDetectorService service;
    private final ObjectMapper       mapper;

    public DrillKafkaConsumer(BotDetectorService service, ObjectMapper mapper) {
        this.service = service;
        this.mapper  = mapper;
    }

    @KafkaListener(topics = "${kafka.topic.security-drills:security_drills}",
                   groupId = "${spring.kafka.consumer.drills-group-id:bot-detector-drills}")
    public void consume(ConsumerRecord<String, String> record) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> payload = mapper.readValue(record.value(), Map.class);
            String action = payload.get("action") instanceof String valor ? valor : "EVALUATE";
            if ("RESET_ALL".equalsIgnoreCase(action)) {
                service.resetAll();
                return;
            }
            String ip = payload.get("ip") instanceof String valor ? valor.strip() : null;
            if (ip == null || ip.isBlank()) {
                return;
            }
            if ("RESET".equalsIgnoreCase(action)) {
                service.reset(ip);
                return;
            }
            int votos = interpretarVotos(payload.get("votes"));
            for (int i = 0; i < votos; i++) {
                service.evaluate(ip, AlertOrigin.DRILL);
            }
        } catch (Exception e) {
            log.warn("Prueba de seguridad descartada (offset {}): {}", record.offset(), e.getMessage());
        }
    }

    private int interpretarVotos(Object valor) {
        int votos = 1;
        if (valor instanceof Number numero) {
            votos = numero.intValue();
        } else if (valor instanceof String texto) {
            try {
                votos = Integer.parseInt(texto.strip());
            } catch (NumberFormatException ignored) {
                votos = 1;
            }
        }
        return Math.max(1, Math.min(MAX_VOTOS_POR_MENSAJE, votos));
    }
}

package com.zerotrust.botdetector.messaging.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zerotrust.botdetector.domain.FraudKind;
import com.zerotrust.botdetector.service.ServicioRegistroFraude;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.Map;

// Evento aparte de raw_votes: estos votos no se aceptaron y el escrutinio no cambia.
@Component
public class FraudEventKafkaConsumer {

    private static final Logger log = LoggerFactory.getLogger(FraudEventKafkaConsumer.class);

    private final ServicioRegistroFraude service;
    private final ObjectMapper       mapper;

    public FraudEventKafkaConsumer(ServicioRegistroFraude service, ObjectMapper mapper) {
        this.service = service;
        this.mapper  = mapper;
    }

    @KafkaListener(topics = "${kafka.topic.fraud-events:fraud_events}",
                   groupId = "${spring.kafka.consumer.fraud-group-id:bot-detector-fraud}")
    public void consume(ConsumerRecord<String, String> record) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> payload = mapper.readValue(record.value(), Map.class);
            String ip = payload.get("ip") instanceof String valor ? valor.strip() : null;
            if (ip == null || ip.isBlank()) {
                return;
            }
            FraudKind kind = interpretarKind(payload.get("kind"));
            service.recordFraud(kind, ip);
        } catch (Exception e) {
            log.warn("Incidente de fraude descartado (offset {}): {}", record.offset(), e.getMessage());
        }
    }

    private FraudKind interpretarKind(Object valor) {
        if (valor instanceof String texto) {
            try {
                return FraudKind.valueOf(texto.strip().toUpperCase());
            } catch (IllegalArgumentException ignored) {
                // Tipo desconocido: no se inventa un incidente.
            }
        }
        return null;
    }
}

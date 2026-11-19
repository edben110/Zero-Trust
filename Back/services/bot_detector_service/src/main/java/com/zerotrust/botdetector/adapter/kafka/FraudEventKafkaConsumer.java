package com.zerotrust.botdetector.adapter.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zerotrust.botdetector.domain.model.FraudKind;
import com.zerotrust.botdetector.service.BotDetectorService;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Consume los incidentes de fraude electoral que detecta voting_api:
 * voto duplicado y voto anulado por región incorrecta.
 *
 * Se mantiene como evento separado de raw_votes porque estos votos no se
 * aceptaron: el escrutinio no cambia, solo se registra el incidente.
 */
@Component
public class FraudEventKafkaConsumer {

    private static final Logger log = LoggerFactory.getLogger(FraudEventKafkaConsumer.class);

    private final BotDetectorService service;
    private final ObjectMapper       mapper;

    public FraudEventKafkaConsumer(BotDetectorService service, ObjectMapper mapper) {
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

package com.zerotrust.botdetector.messaging.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zerotrust.botdetector.service.ServicioDeteccionBots;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class RawVoteKafkaConsumer {

    private static final Logger log = LoggerFactory.getLogger(RawVoteKafkaConsumer.class);

    private final ServicioDeteccionBots service;
    private final ObjectMapper        mapper;

    public RawVoteKafkaConsumer(ServicioDeteccionBots service, ObjectMapper mapper) {
        this.service = service;
        this.mapper  = mapper;
    }

    @KafkaListener(topics = "${kafka.topic.raw-votes:raw_votes}",
                   groupId = "${spring.kafka.consumer.group-id:bot-detector-group}")
    public void consume(ConsumerRecord<String, String> record) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> payload = mapper.readValue(record.value(), Map.class);
            String sourceIp = (String) payload.get("sourceIp");
            if (sourceIp != null && !sourceIp.isBlank()) {
                service.evaluate(sourceIp);
            }
        } catch (Exception e) {
            log.warn("Voto descartado de partición {} offset {}: {}", record.partition(), record.offset(), e.getMessage());
        }
    }
}

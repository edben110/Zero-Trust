package com.zerotrust.botdetector.adapter.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zerotrust.botdetector.service.BotDetectorService;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class RawVoteKafkaConsumer {

    private final BotDetectorService service;
    private final ObjectMapper        mapper;

    public RawVoteKafkaConsumer(BotDetectorService service, ObjectMapper mapper) {
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
            System.err.println("[bot-detector] Error procesando voto: " + e.getMessage());
        }
    }
}

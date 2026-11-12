package com.zerotrust.regionalrollup.adapter.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zerotrust.regionalrollup.domain.model.RawVoteEvent;
import com.zerotrust.regionalrollup.service.RegionalRollupService;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;

@Component
public class RawVoteKafkaConsumer {

    private final RegionalRollupService service;
    private final ObjectMapper          mapper;

    public RawVoteKafkaConsumer(RegionalRollupService service, ObjectMapper mapper) {
        this.service = service;
        this.mapper  = mapper;
    }

    @KafkaListener(topics = "${kafka.topic.raw-votes:raw_votes}",
                   groupId = "${spring.kafka.consumer.group-id:regional-rollup-group}")
    public void consume(ConsumerRecord<String, String> record) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> payload = mapper.readValue(record.value(), Map.class);

            RawVoteEvent event = new RawVoteEvent(
                    (String) payload.get("voteId"),
                    (String) payload.get("voterId"),
                    (String) payload.get("candidateId"),
                    (String) payload.get("region"),
                    (String) payload.get("sourceIp"),
                    Instant.parse((String) payload.get("timestamp"))
            );

            service.processVote(event);
        } catch (Exception e) {
            System.err.println("[regional-rollup] Voto descartado: " + e.getMessage());
        }
    }
}

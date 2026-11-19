package com.zerotrust.voteprocessor.messaging.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zerotrust.voteprocessor.domain.CandidateId;
import com.zerotrust.voteprocessor.domain.RawVoteEvent;
import com.zerotrust.voteprocessor.domain.VoteId;
import com.zerotrust.voteprocessor.domain.VoterId;
import com.zerotrust.voteprocessor.service.VoteProcessorService;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;

@Component
public class RawVoteKafkaConsumer {

    private static final Logger log = LoggerFactory.getLogger(RawVoteKafkaConsumer.class);

    private final VoteProcessorService service;
    private final ObjectMapper         mapper;

    public RawVoteKafkaConsumer(VoteProcessorService service, ObjectMapper mapper) {
        this.service = service;
        this.mapper  = mapper;
    }

    @KafkaListener(topics = "${kafka.topic.raw-votes:raw_votes}",
                   groupId = "${spring.kafka.consumer.group-id:vote-processor-group}")
    public void consume(ConsumerRecord<String, String> record) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> payload = mapper.readValue(record.value(), Map.class);

            RawVoteEvent event = new RawVoteEvent(
                    new VoteId((String) payload.get("voteId")),
                    new VoterId((String) payload.get("voterId")),
                    new CandidateId((String) payload.get("candidateId")),
                    (String) payload.get("region"),
                    (String) payload.get("sourceIp"),
                    Instant.parse((String) payload.get("timestamp"))
            );

            service.processVote(event);
        } catch (Exception e) {
            // Voto malformado: se descarta con log. No se relanza para no bloquear la partición.
            // En producción se enviaría a un dead-letter topic.
            log.warn("Voto descartado de partición {} offset {}: {}", record.partition(), record.offset(), e.getMessage());
        }
    }
}

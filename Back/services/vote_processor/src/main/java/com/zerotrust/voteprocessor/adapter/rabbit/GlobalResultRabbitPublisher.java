package com.zerotrust.voteprocessor.adapter.rabbit;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zerotrust.voteprocessor.domain.model.GlobalResultSnapshot;
import com.zerotrust.voteprocessor.domain.port.GlobalResultPublisher;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Adaptador de salida: serializa el snapshot global y lo publica en el
 * Fanout Exchange live_results_global de RabbitMQ.
 */
@Component
public class GlobalResultRabbitPublisher implements GlobalResultPublisher {

    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper   mapper;
    private final String         exchange;

    public GlobalResultRabbitPublisher(
            RabbitTemplate rabbitTemplate,
            ObjectMapper mapper,
            @Value("${rabbit.exchange.live-results-global:live_results_global}") String exchange) {
        this.rabbitTemplate = rabbitTemplate;
        this.mapper         = mapper;
        this.exchange       = exchange;
    }

    @Override
    public void publish(GlobalResultSnapshot snapshot) {
        try {
            List<Map<String, Object>> results = snapshot.results().stream()
                    .map(r -> Map.<String, Object>of(
                            "candidateId",   r.candidateId().value(),
                            "candidateName", r.candidateName(),
                            "voteCount",     r.voteCount()
                    ))
                    .collect(Collectors.toList());

            Map<String, Object> payload = Map.of(
                    "timestamp", snapshot.timestamp().toString(),
                    "results",   results
            );

            String json = mapper.writeValueAsString(payload);
            // Fanout ignora la routing key; se pasa cadena vacía por convención.
            rabbitTemplate.convertAndSend(exchange, "", json);
        } catch (Exception e) {
            System.err.println("[vote-processor] Error publicando resultado global: " + e.getMessage());
        }
    }
}

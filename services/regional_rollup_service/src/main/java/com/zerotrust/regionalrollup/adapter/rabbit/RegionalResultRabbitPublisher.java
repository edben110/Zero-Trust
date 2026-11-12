package com.zerotrust.regionalrollup.adapter.rabbit;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zerotrust.regionalrollup.domain.model.RegionalResultSnapshot;
import com.zerotrust.regionalrollup.domain.port.RegionalResultPublisher;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Publica el snapshot regional en el Topic Exchange live_results_regional.
 * Routing key = results.<region> según el contrato.
 */
@Component
public class RegionalResultRabbitPublisher implements RegionalResultPublisher {

    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper   mapper;
    private final String         exchange;

    public RegionalResultRabbitPublisher(
            RabbitTemplate rabbitTemplate,
            ObjectMapper mapper,
            @Value("${rabbit.exchange.live-results-regional:live_results_regional}") String exchange) {
        this.rabbitTemplate = rabbitTemplate;
        this.mapper         = mapper;
        this.exchange       = exchange;
    }

    @Override
    public void publish(RegionalResultSnapshot snapshot) {
        try {
            List<Map<String, Object>> results = snapshot.results().stream()
                    .map(r -> Map.<String, Object>of(
                            "candidateId",   r.candidateId(),
                            "candidateName", r.candidateName(),
                            "voteCount",     r.voteCount()
                    ))
                    .collect(Collectors.toList());

            Map<String, Object> payload = Map.of(
                    "region",    snapshot.region(),
                    "timestamp", snapshot.timestamp().toString(),
                    "results",   results
            );

            String json        = mapper.writeValueAsString(payload);
            String routingKey  = "results." + snapshot.region();
            rabbitTemplate.convertAndSend(exchange, routingKey, json);
        } catch (Exception e) {
            System.err.println("[regional-rollup] Error publicando resultado regional: " + e.getMessage());
        }
    }
}

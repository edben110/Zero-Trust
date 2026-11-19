package com.zerotrust.regionalrollup.messaging.rabbit;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zerotrust.regionalrollup.exception.PublicacionFallidaException;
import com.zerotrust.regionalrollup.domain.RegionalResultSnapshot;
import com.zerotrust.regionalrollup.port.RegionalResultPublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class RegionalResultRabbitPublisher implements RegionalResultPublisher {

    private static final Logger log = LoggerFactory.getLogger(RegionalResultRabbitPublisher.class);

    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper   mapper;
    private final String         exchange;
    private final int            intentos;
    private final long           pausaMs;

    public RegionalResultRabbitPublisher(
            RabbitTemplate rabbitTemplate,
            ObjectMapper mapper,
            @Value("${rabbit.exchange.live-results-regional:live_results_regional}") String exchange,
            @Value("${rollup.publicacion.intentos:3}") int intentos,
            @Value("${rollup.publicacion.pausa-ms:200}") long pausaMs) {
        if (intentos < 1) {
            throw new IllegalArgumentException("intentos debe ser al menos 1");
        }
        this.rabbitTemplate = rabbitTemplate;
        this.mapper         = mapper;
        this.exchange       = exchange;
        this.intentos       = intentos;
        this.pausaMs        = pausaMs;
    }

    @Override
    public void publish(RegionalResultSnapshot snapshot) {
        String json       = serializar(snapshot);
        String routingKey = "results." + snapshot.region();
        AmqpException ultimoError = null;
        for (int intento = 1; intento <= intentos; intento++) {
            try {
                rabbitTemplate.convertAndSend(exchange, routingKey, json);
                return;
            } catch (AmqpException e) {
                ultimoError = e;
                log.warn("Intento {}/{} de publicar el resultado de {} fallido: {}",
                        intento, intentos, snapshot.region(), e.getMessage());
                esperar(intento);
            }
        }
        throw new PublicacionFallidaException("No se pudo publicar el resultado regional en " + routingKey, ultimoError);
    }

    private String serializar(RegionalResultSnapshot snapshot) {
        List<Map<String, Object>> results = snapshot.results().stream()
                .map(r -> Map.<String, Object>of(
                        "candidateId",   r.candidateId(),
                        "candidateName", r.candidateName(),
                        "voteCount",     r.voteCount()
                ))
                .toList();
        try {
            return mapper.writeValueAsString(Map.of(
                    "region",    snapshot.region(),
                    "timestamp", snapshot.timestamp().toString(),
                    "results",   results
            ));
        } catch (JsonProcessingException e) {
            throw new PublicacionFallidaException("No se pudo serializar el resultado regional", e);
        }
    }

    private void esperar(int intento) {
        if (intento == intentos || pausaMs <= 0) {
            return;
        }
        try {
            Thread.sleep(pausaMs);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new PublicacionFallidaException("Publicación del resultado regional interrumpida", e);
        }
    }
}

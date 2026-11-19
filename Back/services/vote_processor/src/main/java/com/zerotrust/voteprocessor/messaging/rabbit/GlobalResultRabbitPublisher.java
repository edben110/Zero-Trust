package com.zerotrust.voteprocessor.messaging.rabbit;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zerotrust.voteprocessor.exception.PublicacionFallidaException;
import com.zerotrust.voteprocessor.domain.GlobalResultSnapshot;
import com.zerotrust.voteprocessor.port.GlobalResultPublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * Adaptador de salida: serializa el snapshot global y lo publica en el
 * Fanout Exchange live_results_global de RabbitMQ.
 */
@Component
public class GlobalResultRabbitPublisher implements GlobalResultPublisher {

    private static final Logger log = LoggerFactory.getLogger(GlobalResultRabbitPublisher.class);

    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper   mapper;
    private final String         exchange;
    private final int            intentos;
    private final long           pausaMs;

    public GlobalResultRabbitPublisher(
            RabbitTemplate rabbitTemplate,
            ObjectMapper mapper,
            @Value("${rabbit.exchange.live-results-global:live_results_global}") String exchange,
            @Value("${processor.publicacion.intentos:3}") int intentos,
            @Value("${processor.publicacion.pausa-ms:200}") long pausaMs) {
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
    public void publish(GlobalResultSnapshot snapshot) {
        String json = serializar(snapshot);
        AmqpException ultimoError = null;
        for (int intento = 1; intento <= intentos; intento++) {
            try {
                // Fanout ignora la routing key; se pasa cadena vacía por convención.
                rabbitTemplate.convertAndSend(exchange, "", json);
                return;
            } catch (AmqpException e) {
                ultimoError = e;
                log.warn("Intento {}/{} de publicar el resultado global fallido: {}", intento, intentos, e.getMessage());
                esperar(intento);
            }
        }
        throw new PublicacionFallidaException("No se pudo publicar el resultado global en " + exchange, ultimoError);
    }

    private String serializar(GlobalResultSnapshot snapshot) {
        List<Map<String, Object>> results = snapshot.results().stream()
                .map(r -> Map.<String, Object>of(
                        "candidateId",   r.candidateId().value(),
                        "candidateName", r.candidateName(),
                        "voteCount",     r.voteCount()
                ))
                .toList();
        try {
            return mapper.writeValueAsString(Map.of(
                    "timestamp", snapshot.timestamp().toString(),
                    "results",   results
            ));
        } catch (JsonProcessingException e) {
            throw new PublicacionFallidaException("No se pudo serializar el resultado global", e);
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
            throw new PublicacionFallidaException("Publicación del resultado global interrumpida", e);
        }
    }
}

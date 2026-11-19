package com.zerotrust.botdetector.adapter.kafka;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zerotrust.botdetector.domain.model.SecurityAlert;
import com.zerotrust.botdetector.domain.port.SecurityAlertPublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Publica alertas en security_alerts esperando la confirmación de Kafka.
 *
 * Tras agotar los intentos registra el fallo en ERROR y no relanza: se ejecuta
 * dentro del consumidor de raw_votes y un error haría que el voto se volviera a
 * entregar y se contara dos veces en IpTrafficContext.
 */
@Component
public class SecurityAlertKafkaPublisher implements SecurityAlertPublisher {

    private static final Logger log = LoggerFactory.getLogger(SecurityAlertKafkaPublisher.class);

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper                  mapper;
    private final String                        topic;
    private final int                           intentos;
    private final long                          esperaMs;
    private final long                          pausaMs;

    public SecurityAlertKafkaPublisher(
            KafkaTemplate<String, String> kafkaTemplate,
            ObjectMapper mapper,
            @Value("${kafka.topic.security-alerts:security_alerts}") String topic,
            @Value("${detector.alertas.intentos:3}") int intentos,
            @Value("${detector.alertas.espera-ms:5000}") long esperaMs,
            @Value("${detector.alertas.pausa-ms:200}") long pausaMs) {
        if (intentos < 1) {
            throw new IllegalArgumentException("intentos debe ser al menos 1");
        }
        this.kafkaTemplate = kafkaTemplate;
        this.mapper        = mapper;
        this.topic         = topic;
        this.intentos      = intentos;
        this.esperaMs      = esperaMs;
        this.pausaMs       = pausaMs;
    }

    @Override
    public void publish(SecurityAlert alert) {
        String json = serializar(alert);
        if (json == null) {
            return;
        }
        for (int intento = 1; intento <= intentos; intento++) {
            try {
                kafkaTemplate.send(topic, alert.ip(), json).get(esperaMs, TimeUnit.MILLISECONDS);
                return;
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.error("Publicación de la alerta de {} interrumpida; alerta perdida", alert.ip());
                return;
            } catch (ExecutionException | TimeoutException | RuntimeException e) {
                log.warn("Intento {}/{} de publicar la alerta de {} fallido: {}",
                        intento, intentos, alert.ip(), e.getMessage());
                if (intento < intentos && !pausar()) {
                    return;
                }
            }
        }
        log.error("Alerta de {} ({}, {}) no publicada en {} tras {} intentos",
                alert.ip(), alert.riskLevel(), alert.kind(), topic, intentos);
    }

    private String serializar(SecurityAlert alert) {
        try {
            return mapper.writeValueAsString(Map.of(
                    "ip",            alert.ip(),
                    "riskLevel",     alert.riskLevel(),
                    "voteCount",     alert.voteCount(),
                    "windowSeconds", alert.windowSeconds(),
                    "origin",        alert.origin().name(),
                    "kind",          alert.kind().name(),
                    "detectedAt",    alert.detectedAt().toString()
            ));
        } catch (JsonProcessingException e) {
            log.error("Alerta de {} no serializable; no se publica: {}", alert.ip(), e.getMessage());
            return null;
        }
    }

    private boolean pausar() {
        if (pausaMs <= 0) {
            return true;
        }
        try {
            Thread.sleep(pausaMs);
            return true;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Reintento de alerta interrumpido; alerta perdida");
            return false;
        }
    }
}

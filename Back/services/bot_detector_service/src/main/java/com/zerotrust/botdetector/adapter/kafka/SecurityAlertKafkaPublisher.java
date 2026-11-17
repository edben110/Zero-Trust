package com.zerotrust.botdetector.adapter.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zerotrust.botdetector.domain.model.SecurityAlert;
import com.zerotrust.botdetector.domain.port.SecurityAlertPublisher;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class SecurityAlertKafkaPublisher implements SecurityAlertPublisher {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper                  mapper;
    private final String                        topic;

    public SecurityAlertKafkaPublisher(
            KafkaTemplate<String, String> kafkaTemplate,
            ObjectMapper mapper,
            @Value("${kafka.topic.security-alerts:security_alerts}") String topic) {
        this.kafkaTemplate = kafkaTemplate;
        this.mapper        = mapper;
        this.topic         = topic;
    }

    @Override
    public void publish(SecurityAlert alert) {
        try {
            Map<String, Object> payload = Map.of(
                    "ip",            alert.ip(),
                    "riskLevel",     alert.riskLevel(),
                    "voteCount",     alert.voteCount(),
                    "windowSeconds", alert.windowSeconds(),
                    "detectedAt",    alert.detectedAt().toString()
            );
            String json = mapper.writeValueAsString(payload);
            kafkaTemplate.send(topic, alert.ip(), json);
        } catch (Exception e) {
            System.err.println("[bot-detector] Error publicando alerta: " + e.getMessage());
        }
    }
}

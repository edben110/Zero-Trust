package com.zerotrust.votingapi.messaging.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zerotrust.votingapi.domain.FraudKind;
import com.zerotrust.votingapi.domain.SourceIp;
import com.zerotrust.votingapi.port.RegistroIncidentesFraude;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;

import java.util.LinkedHashMap;
import java.util.Map;

// Sin esperar confirmación: la respuesta al votante no puede depender de que Kafka esté disponible.
public class PublicadorIncidentesFraudeKafka implements RegistroIncidentesFraude {

    private static final Logger LOG = LoggerFactory.getLogger(PublicadorIncidentesFraudeKafka.class);

    private final KafkaTemplate<String, String> plantilla;
    private final String topic;
    private final ObjectMapper json;

    public PublicadorIncidentesFraudeKafka(KafkaTemplate<String, String> plantilla, String topic,
                                           ObjectMapper json) {
        this.plantilla = plantilla;
        this.topic = topic;
        this.json = json;
    }

    @Override
    public void registrar(FraudKind tipo, SourceIp ip) {
        if (tipo == null || ip == null) {
            return;
        }
        try {
            Map<String, Object> evento = new LinkedHashMap<>();
            evento.put("kind", tipo.name());
            evento.put("ip", ip.valor());
            String carga = json.writeValueAsString(evento);
            plantilla.send(topic, ip.valor(), carga)
                    .whenComplete((resultado, error) -> {
                        if (error != null) {
                            LOG.warn("No se pudo publicar el incidente de fraude {}: {}",
                                    tipo, error.getClass().getSimpleName());
                        }
                    });
        } catch (Exception e) {
            // Best-effort: el incidente se pierde, pero la respuesta electoral no cambia.
            LOG.warn("No se pudo serializar el incidente de fraude {}: {}", tipo, e.getClass().getSimpleName());
        }
    }
}

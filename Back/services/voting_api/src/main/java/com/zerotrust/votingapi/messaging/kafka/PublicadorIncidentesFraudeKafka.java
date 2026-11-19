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

/**
 * Publica el incidente en fraud_events. bot_detector_service lo consume y lo
 * guarda en fraud_incidents, de modo que el panel tenga un único origen de
 * verdad y no se escriba la tabla desde dos servicios.
 *
 * Se envía sin esperar confirmación: la respuesta al votante (voto duplicado o
 * anulado) no puede depender de que Kafka esté disponible.
 */
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

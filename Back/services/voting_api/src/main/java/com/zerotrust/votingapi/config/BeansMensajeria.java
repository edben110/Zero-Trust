package com.zerotrust.votingapi.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zerotrust.votingapi.messaging.kafka.PublicadorElegiblesKafka;
import com.zerotrust.votingapi.messaging.kafka.PublicadorIncidentesFraudeKafka;
import com.zerotrust.votingapi.messaging.kafka.PublicadorVotosKafka;
import com.zerotrust.votingapi.messaging.rabbit.ValidadorVotanteRpc;
import com.zerotrust.votingapi.port.PublicadorElegibles;
import com.zerotrust.votingapi.port.PublicadorVotos;
import com.zerotrust.votingapi.port.RegistroIncidentesFraude;
import com.zerotrust.votingapi.port.ValidadorVotante;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaTemplate;

import java.time.Duration;

/**
 * Adaptadores de Kafka y RabbitMQ de la votación y el padrón.
 */
@Configuration
public class BeansMensajeria {

    @Bean
    ValidadorVotante validadorVotante(RabbitTemplate plantilla, ObjectMapper json,
                                      @Value("${votacion.cola-validacion}") String cola) {
        return new ValidadorVotanteRpc(plantilla, cola, json);
    }

    @Bean
    PublicadorVotos publicadorVotos(KafkaTemplate<String, String> plantilla, ObjectMapper json,
                                    @Value("${votacion.topic}") String topic,
                                    @Value("${votacion.publicacion-timeout-ms}") long esperaMs) {
        return new PublicadorVotosKafka(plantilla, topic, json, Duration.ofMillis(esperaMs));
    }

    @Bean
    PublicadorElegibles publicadorElegibles(KafkaTemplate<String, String> plantilla, ObjectMapper json,
                                            @Value("${votacion.topic-padron}") String topic,
                                            @Value("${votacion.publicacion-timeout-ms}") long esperaMs) {
        return new PublicadorElegiblesKafka(plantilla, topic, json, Duration.ofMillis(esperaMs));
    }

    // Voto duplicado y región incorrecta se publican como incidentes de fraude.
    // bot_detector_service los persiste en fraud_incidents (único escritor de la tabla).
    @Bean
    RegistroIncidentesFraude registroIncidentesFraude(KafkaTemplate<String, String> plantilla, ObjectMapper json,
                                                      @Value("${seguridad.fraud-topic:fraud_events}") String topic) {
        return new PublicadorIncidentesFraudeKafka(plantilla, topic, json);
    }
}

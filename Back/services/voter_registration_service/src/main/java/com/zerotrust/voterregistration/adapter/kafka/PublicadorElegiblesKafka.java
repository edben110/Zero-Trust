package com.zerotrust.voterregistration.adapter.kafka;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zerotrust.voterregistration.domain.VotanteRegistrado;
import com.zerotrust.voterregistration.port.PublicadorElegibles;
import org.springframework.kafka.core.KafkaTemplate;

import java.time.Duration;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class PublicadorElegiblesKafka implements PublicadorElegibles {

    private final KafkaTemplate<String, String> plantilla;
    private final String topic;
    private final ObjectMapper json;
    private final Duration espera;

    public PublicadorElegiblesKafka(KafkaTemplate<String, String> plantilla, String topic, ObjectMapper json,
                                    Duration espera) {
        this.plantilla = plantilla;
        this.topic = topic;
        this.json = json;
        this.espera = espera;
    }

    @Override
    public void publicar(VotanteRegistrado votante) {
        try {
            plantilla.send(topic, votante.voterId().valor(), serializar(votante))
                    .get(espera.toMillis(), TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("publicación interrumpida en eligible_voters");
        } catch (ExecutionException | TimeoutException e) {
            throw new IllegalStateException("no se pudo publicar en eligible_voters");
        }
    }

    private String serializar(VotanteRegistrado votante) {
        try {
            return json.writeValueAsString(EventoVotanteElegible.de(votante));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("no se pudo serializar el votante", e);
        }
    }

    // Payload exacto de eligible_voters en docs/contratos.md.
    record EventoVotanteElegible(String voterId, String fullName, String region, String registeredAt) {

        static EventoVotanteElegible de(VotanteRegistrado votante) {
            return new EventoVotanteElegible(votante.voterId().valor(), votante.nombreCompleto().valor(),
                    votante.region().valor(), votante.registradoEn().toString());
        }
    }
}

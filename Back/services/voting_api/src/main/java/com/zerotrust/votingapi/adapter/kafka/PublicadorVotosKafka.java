package com.zerotrust.votingapi.adapter.kafka;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zerotrust.votingapi.exception.PublicacionNoDisponibleException;
import com.zerotrust.votingapi.domain.VotoEmitido;
import com.zerotrust.votingapi.port.PublicadorVotos;
import org.springframework.kafka.core.KafkaTemplate;

import java.time.Duration;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class PublicadorVotosKafka implements PublicadorVotos {

    private final KafkaTemplate<String, String> plantilla;
    private final String topic;
    private final ObjectMapper json;
    private final Duration espera;

    public PublicadorVotosKafka(KafkaTemplate<String, String> plantilla, String topic, ObjectMapper json,
                                Duration espera) {
        this.plantilla = plantilla;
        this.topic = topic;
        this.json = json;
        this.espera = espera;
    }

    // Se espera la confirmación del broker para no responder 200 por un voto que no quedó publicado.
    @Override
    public void publicar(VotoEmitido voto) {
        try {
            plantilla.send(topic, voto.voteId().valor(), serializar(voto)).get(espera.toMillis(), TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new PublicacionNoDisponibleException();
        } catch (ExecutionException | TimeoutException | RuntimeException e) {
            throw new PublicacionNoDisponibleException();
        }
    }

    private String serializar(VotoEmitido voto) {
        try {
            return json.writeValueAsString(EventoRawVote.de(voto));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("no se pudo serializar el voto", e);
        }
    }
}

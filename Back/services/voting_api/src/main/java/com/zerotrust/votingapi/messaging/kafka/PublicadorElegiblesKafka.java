package com.zerotrust.votingapi.messaging.kafka;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zerotrust.votingapi.exception.PublicacionNoDisponibleException;
import com.zerotrust.votingapi.domain.VotanteInscrito;
import com.zerotrust.votingapi.port.PublicadorElegibles;
import org.springframework.kafka.core.KafkaTemplate;

import java.time.Duration;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

// Mismo mensaje que publica voter_registration_service en eligible_voters (docs/contratos.md), con key = voterId.
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

    // Se espera la confirmación del broker para no dar por inscrito a alguien que no llegó a eligible_voters.
    @Override
    public void publicar(VotanteInscrito votante) {
        try {
            plantilla.send(topic, votante.voterId().valor(), serializar(votante))
                    .get(espera.toMillis(), TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new PublicacionNoDisponibleException();
        } catch (ExecutionException | TimeoutException | RuntimeException e) {
            throw new PublicacionNoDisponibleException();
        }
    }

    private String serializar(VotanteInscrito votante) {
        try {
            return json.writeValueAsString(new EventoVotanteElegible(votante.voterId().valor(),
                    votante.region().valor(), votante.inscritoEn().toString()));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("no se pudo serializar el votante", e);
        }
    }

    record EventoVotanteElegible(String voterId, String region, String registeredAt) {
    }
}

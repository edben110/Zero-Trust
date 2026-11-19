package com.zerotrust.votingapi.messaging.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zerotrust.votingapi.domain.CandidateId;
import com.zerotrust.votingapi.exception.PublicacionNoDisponibleException;
import com.zerotrust.votingapi.domain.Region;
import com.zerotrust.votingapi.domain.SourceIp;
import com.zerotrust.votingapi.domain.VoteId;
import com.zerotrust.votingapi.domain.VoterId;
import com.zerotrust.votingapi.domain.VotoEmitido;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("PublicadorVotosKafka")
class PublicadorVotosKafkaTest {

    private static final String VOTANTE = "3f2b8c1e-6d4a-4b7e-9c21-0a5d6e7f8a9b";
    private static final ObjectMapper JSON = new ObjectMapper();

    @Mock
    private KafkaTemplate<String, String> plantilla;

    private final VotoEmitido voto = new VotoEmitido(new VoteId("11111111-2222-4333-8444-555555555555"),
            new VoterId(VOTANTE), new CandidateId("0b1c2d3e-4f50-4a61-8b72-93a4b5c6d7e8"), new Region("norte"),
            new SourceIp("203.0.113.7"), Instant.parse("2026-11-14T15:00:00Z"));

    private PublicadorVotosKafka publicador() {
        return new PublicadorVotosKafka(plantilla, "raw_votes", JSON, Duration.ofSeconds(5));
    }

    @Test
    @DisplayName("Debe publicar en raw_votes con key = voteId y el payload exacto del contrato")
    void publica() throws Exception {
        ArgumentCaptor<String> valor = ArgumentCaptor.forClass(String.class);
        when(plantilla.send(eq("raw_votes"), eq("11111111-2222-4333-8444-555555555555"), valor.capture()))
                .thenReturn(CompletableFuture.completedFuture(new SendResult<>(null, null)));

        publicador().publicar(voto);

        verify(plantilla, times(1)).send(anyString(), anyString(), anyString());
        assertThat(valor.getValue()).isEqualTo("{\"voteId\":\"11111111-2222-4333-8444-555555555555\","
                + "\"voterId\":\"" + VOTANTE + "\",\"candidateId\":\"0b1c2d3e-4f50-4a61-8b72-93a4b5c6d7e8\","
                + "\"region\":\"norte\",\"sourceIp\":\"203.0.113.7\",\"timestamp\":\"2026-11-14T15:00:00Z\"}");
    }

    @Test
    @DisplayName("Si Kafka falla, la publicación no está disponible")
    void falla() {
        when(plantilla.send(anyString(), anyString(), anyString()))
                .thenReturn(CompletableFuture.failedFuture(new IllegalStateException("broker caído")));

        assertThrows(PublicacionNoDisponibleException.class, () -> publicador().publicar(voto));
    }

    @Test
    @DisplayName("Si send lanza directamente, la publicación no está disponible")
    void fallaAlEnviar() {
        when(plantilla.send(anyString(), anyString(), anyString()))
                .thenThrow(new org.apache.kafka.common.KafkaException("sin metadatos"));

        assertThrows(PublicacionNoDisponibleException.class, () -> publicador().publicar(voto));
    }
}

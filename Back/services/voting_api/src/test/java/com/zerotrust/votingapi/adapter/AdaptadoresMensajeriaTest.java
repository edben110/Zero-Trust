package com.zerotrust.votingapi.adapter;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zerotrust.votingapi.adapter.kafka.PublicadorVotosKafka;
import com.zerotrust.votingapi.adapter.rabbit.ValidadorVotanteRpc;
import com.zerotrust.votingapi.domain.CandidateId;
import com.zerotrust.votingapi.domain.PublicacionNoDisponibleException;
import com.zerotrust.votingapi.domain.Region;
import com.zerotrust.votingapi.domain.SourceIp;
import com.zerotrust.votingapi.domain.ValidacionNoDisponibleException;
import com.zerotrust.votingapi.domain.VoteId;
import com.zerotrust.votingapi.domain.VoterId;
import com.zerotrust.votingapi.domain.VotoEmitido;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.AmqpConnectException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageBuilder;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

import java.net.ConnectException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdaptadoresMensajeriaTest {

    private static final String VOTANTE = "3f2b8c1e-6d4a-4b7e-9c21-0a5d6e7f8a9b";
    private static final ObjectMapper JSON = new ObjectMapper();

    @Nested
    @DisplayName("ValidadorVotanteRpc")
    class Rpc {

        @Mock
        private RabbitTemplate plantilla;

        private ValidadorVotanteRpc validador() {
            return new ValidadorVotanteRpc(plantilla, "user_validation_queue", JSON);
        }

        private static Message respuesta(String cuerpo) {
            return MessageBuilder.withBody(cuerpo.getBytes(StandardCharsets.UTF_8)).build();
        }

        @Test
        @DisplayName("Debe enviar {\"voterId\"} a user_validation_queue y leer {\"valid\":true}")
        void habilitado() throws Exception {
            ArgumentCaptor<Message> peticion = ArgumentCaptor.forClass(Message.class);
            when(plantilla.sendAndReceive(eq(""), eq("user_validation_queue"), peticion.capture()))
                    .thenReturn(respuesta("{\"valid\":true}"));

            assertThat(validador().estaHabilitado(new VoterId(VOTANTE))).isTrue();

            JsonNode cuerpo = JSON.readTree(peticion.getValue().getBody());
            assertThat(cuerpo.get("voterId").asText()).isEqualTo(VOTANTE);
            assertThat(cuerpo.size()).isEqualTo(1);
            assertThat(peticion.getValue().getMessageProperties().getContentType()).isEqualTo("application/json");
        }

        @Test
        @DisplayName("Debe leer {\"valid\":false}")
        void noHabilitado() {
            when(plantilla.sendAndReceive(anyString(), anyString(), any(Message.class)))
                    .thenReturn(respuesta("{\"valid\":false}"));

            assertThat(validador().estaHabilitado(new VoterId(VOTANTE))).isFalse();
        }

        @Test
        @DisplayName("Sin respuesta a tiempo, con el broker caído o con respuesta ilegible, la validación no está disponible")
        void noDisponible() {
            when(plantilla.sendAndReceive(anyString(), anyString(), any(Message.class))).thenReturn(null);
            assertThrows(ValidacionNoDisponibleException.class, () -> validador().estaHabilitado(new VoterId(VOTANTE)));

            when(plantilla.sendAndReceive(anyString(), anyString(), any(Message.class)))
                    .thenThrow(new AmqpConnectException(new ConnectException("sin broker")));
            assertThrows(ValidacionNoDisponibleException.class, () -> validador().estaHabilitado(new VoterId(VOTANTE)));
        }

        @Test
        @DisplayName("Una respuesta ilegible o sin booleano no se toma como positiva")
        void respuestaIlegible() {
            for (String cuerpo : new String[] {"no es json", "{}", "{\"valid\":\"true\"}", "\"valido\""}) {
                when(plantilla.sendAndReceive(anyString(), anyString(), any(Message.class))).thenReturn(respuesta(cuerpo));
                assertThrows(ValidacionNoDisponibleException.class, () -> validador().estaHabilitado(new VoterId(VOTANTE)));
            }
        }
    }

    @Nested
    @DisplayName("PublicadorVotosKafka")
    class Kafka {

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
}

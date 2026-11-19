package com.zerotrust.votingapi.messaging.kafka;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zerotrust.votingapi.exception.PublicacionNoDisponibleException;
import com.zerotrust.votingapi.domain.Region;
import com.zerotrust.votingapi.domain.VotanteInscrito;
import com.zerotrust.votingapi.domain.VoterId;
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
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("PublicadorElegiblesKafka")
class PublicadorElegiblesKafkaTest {

    private static final Instant AHORA = Instant.parse("2026-11-18T10:00:00Z");
    private static final VotanteInscrito VOTANTE = new VotanteInscrito(
            new VoterId("fae9cb04-ba2b-87ef-882c-f39dd70dbaa1"), new Region("norte"),
            AHORA);

    @Mock
    private KafkaTemplate<String, String> plantilla;

    private PublicadorElegiblesKafka publicador() {
        return new PublicadorElegiblesKafka(plantilla, "eligible_voters", new ObjectMapper(), Duration.ofSeconds(1));
    }

    @Test
    @DisplayName("Debe publicar en eligible_voters con key = voterId y el payload del contrato")
    void publica() throws Exception {
        ArgumentCaptor<String> valor = ArgumentCaptor.forClass(String.class);
        when(plantilla.send(eq("eligible_voters"), eq(VOTANTE.voterId().valor()), valor.capture()))
                .thenReturn(CompletableFuture.completedFuture(new SendResult<>(null, null)));

        publicador().publicar(VOTANTE);

        JsonNode evento = new ObjectMapper().readTree(valor.getValue());
        assertThat(evento.get("voterId").asText()).isEqualTo("fae9cb04-ba2b-87ef-882c-f39dd70dbaa1");
        assertThat(evento.has("fullName")).isFalse();
        assertThat(evento.get("region").asText()).isEqualTo("norte");
        assertThat(evento.get("registeredAt").asText()).isEqualTo("2026-11-18T10:00:00Z");
        assertThat(evento.size()).isEqualTo(3);
    }

    @Test
    @DisplayName("Si el broker no confirma, debe lanzar PublicacionNoDisponibleException")
    void falla() {
        when(plantilla.send(anyString(), anyString(), anyString()))
                .thenReturn(CompletableFuture.failedFuture(new IllegalStateException("broker caído")));

        assertThrows(PublicacionNoDisponibleException.class, () -> publicador().publicar(VOTANTE));
    }
}

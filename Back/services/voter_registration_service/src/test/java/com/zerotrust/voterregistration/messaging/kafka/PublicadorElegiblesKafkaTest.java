package com.zerotrust.voterregistration.messaging.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zerotrust.voterregistration.domain.Region;
import com.zerotrust.voterregistration.domain.VotanteRegistrado;
import com.zerotrust.voterregistration.domain.VoterId;
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

@DisplayName("PublicadorElegiblesKafka")
@ExtendWith(MockitoExtension.class)
class PublicadorElegiblesKafkaTest {

    private static final String ID_1 = "3f2b8c1e-6d4a-4b7e-9c21-0a5d6e7f8a9b";
    private static final Instant AHORA = Instant.parse("2026-11-14T08:00:00Z");

    private static VotanteRegistrado votante(String id) {
        return new VotanteRegistrado(new VoterId(id), new Region("norte"), AHORA);
    }

    @Mock
    private KafkaTemplate<String, String> plantilla;

    private PublicadorElegiblesKafka publicador() {
        return new PublicadorElegiblesKafka(plantilla, "eligible_voters", new ObjectMapper(), Duration.ofSeconds(5));
    }

    @Test
    @DisplayName("Debe publicar con key = voterId y el payload exacto del contrato")
    void publica() {
        ArgumentCaptor<String> valor = ArgumentCaptor.forClass(String.class);
        when(plantilla.send(eq("eligible_voters"), eq(ID_1), valor.capture()))
                .thenReturn(CompletableFuture.completedFuture(new SendResult<>(null, null)));

        publicador().publicar(votante(ID_1));

        assertThat(valor.getValue()).isEqualTo("{\"voterId\":\"" + ID_1
                + "\",\"region\":\"norte\",\"registeredAt\":\"2026-11-14T08:00:00Z\"}");
    }

    @Test
    @DisplayName("Si Kafka falla debe lanzar un error para terminar con código distinto de 0")
    void falla() {
        when(plantilla.send(anyString(), anyString(), anyString()))
                .thenReturn(CompletableFuture.failedFuture(new IllegalStateException("broker caído")));

        assertThrows(IllegalStateException.class, () -> publicador().publicar(votante(ID_1)));
    }
}

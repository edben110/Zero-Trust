package com.zerotrust.votingapi.adapter;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zerotrust.votingapi.adapter.kafka.PublicadorElegiblesKafka;
import com.zerotrust.votingapi.adapter.persistence.RepositorioPadronJdbc;
import com.zerotrust.votingapi.domain.NombreCompleto;
import com.zerotrust.votingapi.domain.PublicacionNoDisponibleException;
import com.zerotrust.votingapi.domain.Region;
import com.zerotrust.votingapi.domain.VotanteInscrito;
import com.zerotrust.votingapi.domain.VotanteYaInscritoException;
import com.zerotrust.votingapi.domain.VoterId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdaptadoresPadronTest {

    private static final Instant AHORA = Instant.parse("2026-11-18T10:00:00Z");
    private static final VotanteInscrito VOTANTE = new VotanteInscrito(
            new VoterId("fae9cb04-ba2b-87ef-882c-f39dd70dbaa1"), new NombreCompleto("Ana Pérez"), new Region("norte"),
            AHORA);

    @Nested
    @DisplayName("RepositorioPadronJdbc")
    class Repositorio {

        @Mock
        private JdbcTemplate jdbc;

        @Test
        @DisplayName("Debe insertar en registered_voters el UUID, el nombre, la región y la fecha, sin la cédula")
        void inserta() {
            when(jdbc.update(anyString(), any(Object[].class))).thenReturn(1);
            ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
            ArgumentCaptor<Object[]> parametros = ArgumentCaptor.forClass(Object[].class);

            new RepositorioPadronJdbc(jdbc).inscribir(VOTANTE);

            verify(jdbc).update(sql.capture(), parametros.capture());
            assertThat(sql.getValue()).contains("registered_voters").contains("ON CONFLICT (voter_id) DO NOTHING");
            assertThat(parametros.getValue()).containsExactly("fae9cb04-ba2b-87ef-882c-f39dd70dbaa1", "Ana Pérez",
                    "norte", Timestamp.from(AHORA));
        }

        @Test
        @DisplayName("Si el voterId ya existía (0 filas), debe lanzar VotanteYaInscritoException")
        void yaInscrito() {
            when(jdbc.update(anyString(), any(Object[].class))).thenReturn(0);

            assertThrows(VotanteYaInscritoException.class, () -> new RepositorioPadronJdbc(jdbc).inscribir(VOTANTE));
        }
    }

    @Nested
    @DisplayName("PublicadorElegiblesKafka")
    class Kafka {

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
            assertThat(evento.get("fullName").asText()).isEqualTo("Ana Pérez");
            assertThat(evento.get("region").asText()).isEqualTo("norte");
            assertThat(evento.get("registeredAt").asText()).isEqualTo("2026-11-18T10:00:00Z");
            assertThat(evento.size()).isEqualTo(4);
        }

        @Test
        @DisplayName("Si el broker no confirma, debe lanzar PublicacionNoDisponibleException")
        void falla() {
            when(plantilla.send(anyString(), anyString(), anyString()))
                    .thenReturn(CompletableFuture.failedFuture(new IllegalStateException("broker caído")));

            assertThrows(PublicacionNoDisponibleException.class, () -> publicador().publicar(VOTANTE));
        }
    }
}

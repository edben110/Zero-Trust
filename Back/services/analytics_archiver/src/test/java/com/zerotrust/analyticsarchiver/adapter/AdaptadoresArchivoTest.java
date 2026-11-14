package com.zerotrust.analyticsarchiver.adapter;

import com.zerotrust.analyticsarchiver.adapter.kafka.ConsumidorRawVotes;
import com.zerotrust.analyticsarchiver.adapter.kafka.LectorVotoCrudo;
import com.zerotrust.analyticsarchiver.adapter.persistence.MapperVotoAuditado;
import com.zerotrust.analyticsarchiver.adapter.persistence.RepositorioAuditoriaJpa;
import com.zerotrust.analyticsarchiver.adapter.persistence.VotoAuditadoEntity;
import com.zerotrust.analyticsarchiver.adapter.persistence.VotoAuditadoJpaRepository;
import com.zerotrust.analyticsarchiver.config.PoliticaReintentos;
import com.zerotrust.analyticsarchiver.domain.CandidateId;
import com.zerotrust.analyticsarchiver.domain.Region;
import com.zerotrust.analyticsarchiver.domain.SourceIp;
import com.zerotrust.analyticsarchiver.domain.VoteId;
import com.zerotrust.analyticsarchiver.domain.VoterId;
import com.zerotrust.analyticsarchiver.domain.VotoAuditado;
import com.zerotrust.analyticsarchiver.service.ServicioArchivoVotos;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.util.backoff.FixedBackOff;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class AdaptadoresArchivoTest {

    private static final String VOTE = "11111111-2222-4333-8444-555555555555";
    private static final String PAYLOAD = "{\"voteId\":\"" + VOTE + "\",\"voterId\":\"3f2b8c1e-6d4a-4b7e-9c21-0a5d6e7f8a9b\","
            + "\"candidateId\":\"0b1c2d3e-4f50-4a61-8b72-93a4b5c6d7e8\",\"region\":\"norte\","
            + "\"sourceIp\":\"203.0.113.7\",\"timestamp\":\"2026-11-14T15:00:00Z\"}";
    private static final VotoAuditado VOTO = new VotoAuditado(new VoteId(VOTE),
            new VoterId("3f2b8c1e-6d4a-4b7e-9c21-0a5d6e7f8a9b"), new CandidateId("0b1c2d3e-4f50-4a61-8b72-93a4b5c6d7e8"),
            new Region("norte"), new SourceIp("203.0.113.7"), Instant.parse("2026-11-14T15:00:00Z"));

    @Nested
    @DisplayName("LectorVotoCrudo")
    class Lector {

        private final LectorVotoCrudo lector = new LectorVotoCrudo();

        @Test
        @DisplayName("Debe leer el payload de raw_votes")
        void lee() {
            assertThat(lector.interpretar(VOTE, PAYLOAD)).contains(VOTO);
            assertThat(lector.interpretar(null, PAYLOAD)).contains(VOTO);
        }

        @ParameterizedTest
        @ValueSource(strings = {"no es json", "{}", "[]",
                "{\"voteId\":\"x\",\"voterId\":\"3f2b8c1e-6d4a-4b7e-9c21-0a5d6e7f8a9b\",\"candidateId\":\"0b1c2d3e-4f50-4a61-8b72-93a4b5c6d7e8\",\"region\":\"norte\",\"sourceIp\":\"1.1.1.1\",\"timestamp\":\"2026-11-14T15:00:00Z\"}",
                "{\"voteId\":\"11111111-2222-4333-8444-555555555555\",\"voterId\":\"3f2b8c1e-6d4a-4b7e-9c21-0a5d6e7f8a9b\",\"candidateId\":\"0b1c2d3e-4f50-4a61-8b72-93a4b5c6d7e8\",\"region\":\"norte\",\"sourceIp\":\"1.1.1.1\",\"timestamp\":\"ayer\"}",
                "{\"voteId\":\"11111111-2222-4333-8444-555555555555\",\"voterId\":\"3f2b8c1e-6d4a-4b7e-9c21-0a5d6e7f8a9b\",\"candidateId\":\"0b1c2d3e-4f50-4a61-8b72-93a4b5c6d7e8\",\"sourceIp\":\"1.1.1.1\",\"timestamp\":\"2026-11-14T15:00:00Z\"}"})
        @DisplayName("Un mensaje malformado se descarta")
        void malformado(String valor) {
            assertThat(lector.interpretar(VOTE, valor)).isEmpty();
            assertThat(lector.interpretar(VOTE, null)).isEmpty();
        }

        @Test
        @DisplayName("Una key distinta del voteId se descarta")
        void keyDistinta() {
            assertThat(lector.interpretar("9a8b7c6d-5e4f-4a3b-8c2d-1e0f2a3b4c5d", PAYLOAD)).isEmpty();
        }
    }

    @Nested
    @DisplayName("ConsumidorRawVotes")
    @ExtendWith(MockitoExtension.class)
    class Consumidor {

        @Mock
        private ServicioArchivoVotos servicio;

        private ConsumidorRawVotes consumidor() {
            return new ConsumidorRawVotes(servicio, new LectorVotoCrudo());
        }

        @Test
        @DisplayName("Debe archivar exactamente una vez cada voto válido")
        void archiva() {
            consumidor().recibir(new ConsumerRecord<>("raw_votes", 0, 3L, VOTE, PAYLOAD));

            verify(servicio, times(1)).archivar(VOTO);
        }

        @Test
        @DisplayName("Un mensaje malformado se descarta sin tocar la base de datos")
        void descarta() {
            consumidor().recibir(new ConsumerRecord<>("raw_votes", 0, 3L, VOTE, "no es json"));

            verifyNoInteractions(servicio);
        }

        @Test
        @DisplayName("Un fallo de la base de datos se propaga para que no se confirme el offset")
        void propagaFallo() {
            when(servicio.archivar(any())).thenThrow(new IllegalStateException("sin base de datos"));

            assertThrows(IllegalStateException.class,
                    () -> consumidor().recibir(new ConsumerRecord<>("raw_votes", 0, 3L, VOTE, PAYLOAD)));
        }
    }

    @Nested
    @DisplayName("Persistencia")
    @ExtendWith(MockitoExtension.class)
    class Persistencia {

        @Mock
        private VotoAuditadoJpaRepository jpa;

        @Test
        @DisplayName("El mapper convierte en ambos sentidos sin perder datos")
        void mapper() {
            VotoAuditadoEntity entidad = MapperVotoAuditado.aEntidad(VOTO);

            assertThat(entidad.getVoteId()).isEqualTo(VOTE);
            assertThat(MapperVotoAuditado.aDominio(entidad)).isEqualTo(VOTO);
        }

        @Test
        @DisplayName("Debe insertar un voto nuevo una sola vez")
        void insertaNuevo() {
            when(jpa.existsByVoteId(VOTE)).thenReturn(false);

            assertThat(new RepositorioAuditoriaJpa(jpa).guardarSiNoExiste(VOTO)).isTrue();

            verify(jpa, times(1)).save(any(VotoAuditadoEntity.class));
        }

        @Test
        @DisplayName("No debe insertar un voto que ya existe")
        void noDuplica() {
            when(jpa.existsByVoteId(VOTE)).thenReturn(true);

            assertThat(new RepositorioAuditoriaJpa(jpa).guardarSiNoExiste(VOTO)).isFalse();

            verify(jpa, never()).save(any());
        }
    }

    @Test
    @DisplayName("La política de reintentos no tiene límite, para no confirmar offsets mientras la BD falle")
    void reintentosSinLimite() {
        FixedBackOff politica = PoliticaReintentos.mientrasFalleLaBaseDeDatos();

        assertThat(politica.getMaxAttempts()).isEqualTo(FixedBackOff.UNLIMITED_ATTEMPTS);
        assertThat(politica.getInterval()).isPositive();
    }
}

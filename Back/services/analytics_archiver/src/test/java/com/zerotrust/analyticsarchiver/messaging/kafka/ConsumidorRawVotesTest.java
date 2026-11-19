package com.zerotrust.analyticsarchiver.messaging.kafka;

import com.zerotrust.analyticsarchiver.domain.CandidateId;
import com.zerotrust.analyticsarchiver.domain.Region;
import com.zerotrust.analyticsarchiver.domain.SourceIp;
import com.zerotrust.analyticsarchiver.domain.VoteId;
import com.zerotrust.analyticsarchiver.domain.VoterId;
import com.zerotrust.analyticsarchiver.domain.VotoAuditado;
import com.zerotrust.analyticsarchiver.service.ServicioArchivoVotos;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@DisplayName("ConsumidorRawVotes")
@ExtendWith(MockitoExtension.class)
class ConsumidorRawVotesTest {

    private static final String VOTE = "11111111-2222-4333-8444-555555555555";
    private static final String PAYLOAD = "{\"voteId\":\"" + VOTE + "\",\"voterId\":\"3f2b8c1e-6d4a-4b7e-9c21-0a5d6e7f8a9b\","
            + "\"candidateId\":\"0b1c2d3e-4f50-4a61-8b72-93a4b5c6d7e8\",\"region\":\"norte\","
            + "\"sourceIp\":\"203.0.113.7\",\"timestamp\":\"2026-11-14T15:00:00Z\"}";
    private static final VotoAuditado VOTO = new VotoAuditado(new VoteId(VOTE),
            new VoterId("3f2b8c1e-6d4a-4b7e-9c21-0a5d6e7f8a9b"), new CandidateId("0b1c2d3e-4f50-4a61-8b72-93a4b5c6d7e8"),
            new Region("norte"), new SourceIp("203.0.113.7"), Instant.parse("2026-11-14T15:00:00Z"));

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

package com.zerotrust.voteprocessor.repository.memoria;

import com.zerotrust.voteprocessor.domain.CandidateId;
import com.zerotrust.voteprocessor.domain.CandidateResult;
import com.zerotrust.voteprocessor.domain.RawVoteEvent;
import com.zerotrust.voteprocessor.domain.VoteAggregate;
import com.zerotrust.voteprocessor.domain.VoteId;
import com.zerotrust.voteprocessor.domain.VoterId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ConteoEnMemoriaTest {

    private static RawVoteEvent voto(String voterId, String candidato) {
        return new RawVoteEvent(new VoteId(UUID.randomUUID().toString()), new VoterId(voterId),
                new CandidateId(candidato), "norte", "10.0.0.1", Instant.now());
    }

    @Test
    @DisplayName("Debe contar los votos registrados ignorando al votante repetido")
    void cuentaVotosRegistrados() {
        // Arrange
        ConteoEnMemoria conteo = new ConteoEnMemoria(new VoteAggregate());
        String votante = UUID.randomUUID().toString();
        String candidato = UUID.randomUUID().toString();

        // Act
        conteo.registrar(voto(votante, candidato));
        conteo.registrar(voto(votante, candidato));
        List<CandidateResult> resultado = conteo.conteoActual();

        // Assert
        assertEquals(List.of(new CandidateResult(new CandidateId(candidato), candidato, 1)), resultado);
    }
}

package com.zerotrust.voteprocessor.domain;

import com.zerotrust.voteprocessor.domain.model.CandidateId;
import com.zerotrust.voteprocessor.domain.model.GlobalResultSnapshot;
import com.zerotrust.voteprocessor.domain.model.RawVoteEvent;
import com.zerotrust.voteprocessor.domain.model.VoteAggregate;
import com.zerotrust.voteprocessor.domain.model.VoteId;
import com.zerotrust.voteprocessor.domain.model.VoterId;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pruebas de dominio puro. Sin Spring, sin brokers, sin BD.
 * Patrón AAA: Arrange → Act → Assert.
 */
class VoteAggregateTest {

    private RawVoteEvent buildEvent(String voterId, String candidateId) {
        return new RawVoteEvent(
                new VoteId(UUID.randomUUID().toString()),
                new VoterId(voterId),
                new CandidateId(candidateId),
                "norte",
                "10.0.0.1",
                Instant.now()
        );
    }

    @Test
    void primerVotoEsAceptado() {
        // Arrange
        VoteAggregate aggregate = new VoteAggregate();
        RawVoteEvent  event     = buildEvent(UUID.randomUUID().toString(), UUID.randomUUID().toString());

        // Act
        boolean accepted = aggregate.accept(event);

        // Assert
        assertTrue(accepted);
        assertEquals(1, aggregate.totalVotes());
    }

    @Test
    void votoDuplicadoEsIgnorado() {
        // Arrange
        VoteAggregate aggregate = new VoteAggregate();
        String voterId     = UUID.randomUUID().toString();
        String candidateId = UUID.randomUUID().toString();
        aggregate.accept(buildEvent(voterId, candidateId));

        // Act: mismo voterId, mismo candidato
        boolean accepted = aggregate.accept(buildEvent(voterId, candidateId));

        // Assert
        assertFalse(accepted);
        assertEquals(1, aggregate.totalVotes());
    }

    @Test
    void conteoAcumulaPorCandidato() {
        // Arrange
        VoteAggregate aggregate  = new VoteAggregate();
        String candidateA = UUID.randomUUID().toString();
        String candidateB = UUID.randomUUID().toString();

        // Act
        aggregate.accept(buildEvent(UUID.randomUUID().toString(), candidateA));
        aggregate.accept(buildEvent(UUID.randomUUID().toString(), candidateA));
        aggregate.accept(buildEvent(UUID.randomUUID().toString(), candidateB));

        // Assert
        GlobalResultSnapshot snapshot = aggregate.snapshot();
        assertEquals(2, snapshot.results().size());

        long votosA = snapshot.results().stream()
                .filter(r -> r.candidateId().value().equals(candidateA))
                .mapToLong(r -> r.voteCount())
                .sum();
        assertEquals(2, votosA);
    }

    @Test
    void snapshotEsInmutable() {
        // Arrange
        VoteAggregate aggregate = new VoteAggregate();
        aggregate.accept(buildEvent(UUID.randomUUID().toString(), UUID.randomUUID().toString()));

        // Act & Assert: intentar modificar la lista lanza excepción
        GlobalResultSnapshot snapshot = aggregate.snapshot();
        assertThrows(UnsupportedOperationException.class,
                () -> snapshot.results().add(null));
    }

    @Test
    void voteIdNuloLanzaExcepcion() {
        assertThrows(IllegalArgumentException.class, () -> new VoteId(null));
    }

    @Test
    void voteIdVacioLanzaExcepcion() {
        assertThrows(IllegalArgumentException.class, () -> new VoteId("  "));
    }

    @Test
    void voteIdMalformadoLanzaExcepcion() {
        assertThrows(Exception.class, () -> new VoteId("no-es-uuid"));
    }
}

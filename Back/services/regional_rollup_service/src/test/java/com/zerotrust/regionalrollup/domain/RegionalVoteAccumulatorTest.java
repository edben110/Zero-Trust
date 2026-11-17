package com.zerotrust.regionalrollup.domain;

import com.zerotrust.regionalrollup.domain.model.RawVoteEvent;
import com.zerotrust.regionalrollup.domain.model.RegionalCandidateResult;
import com.zerotrust.regionalrollup.domain.model.RegionalResultSnapshot;
import com.zerotrust.regionalrollup.domain.model.RegionalVoteAccumulator;
import com.zerotrust.regionalrollup.domain.strategy.SimpleCountStrategy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class RegionalVoteAccumulatorTest {

    private RegionalVoteAccumulator accumulator;

    @BeforeEach
    void setUp() {
        accumulator = new RegionalVoteAccumulator(new SimpleCountStrategy());
    }

    private RawVoteEvent event(String region, String voterId, String candidateId) {
        return new RawVoteEvent(
                UUID.randomUUID().toString(), voterId, candidateId,
                region, "10.0.0.1", Instant.now()
        );
    }

    @Test
    void acumulaVotosPorRegion() {
        // Arrange
        String norte = "norte";
        String sur   = "sur";
        String cA    = UUID.randomUUID().toString();

        // Act
        accumulator.accept(event(norte, UUID.randomUUID().toString(), cA));
        accumulator.accept(event(norte, UUID.randomUUID().toString(), cA));
        accumulator.accept(event(sur,   UUID.randomUUID().toString(), cA));

        // Assert
        RegionalResultSnapshot snapshotNorte = accumulator.snapshotFor(norte);
        assertEquals(1, snapshotNorte.results().size());
        assertEquals(2L, snapshotNorte.results().get(0).voteCount());

        RegionalResultSnapshot snapshotSur = accumulator.snapshotFor(sur);
        assertEquals(1L, snapshotSur.results().get(0).voteCount());
    }

    @Test
    void duplicadoEnMismaRegionEsIgnorado() {
        // Arrange
        String voterId = UUID.randomUUID().toString();
        String cA      = UUID.randomUUID().toString();

        accumulator.accept(event("norte", voterId, cA));

        // Act
        boolean accepted = accumulator.accept(event("norte", voterId, cA));

        // Assert
        assertFalse(accepted);
        assertEquals(1L, accumulator.snapshotFor("norte").results().get(0).voteCount());
    }

    @Test
    void regionesActivasSeActualizan() {
        // Arrange & Act
        accumulator.accept(event("norte", UUID.randomUUID().toString(), UUID.randomUUID().toString()));
        accumulator.accept(event("sur",   UUID.randomUUID().toString(), UUID.randomUUID().toString()));

        // Assert
        assertTrue(accumulator.activeRegions().containsAll(java.util.Set.of("norte", "sur")));
    }

    @Test
    void simpleCountStrategyOrdenaDescendente() {
        // Arrange
        String cA = UUID.randomUUID().toString();
        String cB = UUID.randomUUID().toString();
        accumulator.accept(event("norte", UUID.randomUUID().toString(), cB)); // 1 voto para B
        accumulator.accept(event("norte", UUID.randomUUID().toString(), cA)); // 1 voto para A
        accumulator.accept(event("norte", UUID.randomUUID().toString(), cA)); // 2 votos para A

        // Act
        RegionalResultSnapshot snapshot = accumulator.snapshotFor("norte");

        // Assert: A debe ir primero (mayor conteo)
        RegionalCandidateResult primero = snapshot.results().get(0);
        assertEquals(cA, primero.candidateId());
        assertEquals(2L, primero.voteCount());
    }
}

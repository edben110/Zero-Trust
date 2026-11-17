package com.zerotrust.regionalrollup.service;

import com.zerotrust.regionalrollup.domain.model.RawVoteEvent;
import com.zerotrust.regionalrollup.domain.model.RegionalVoteAccumulator;
import com.zerotrust.regionalrollup.domain.port.RegionalResultPublisher;
import com.zerotrust.regionalrollup.domain.strategy.SimpleCountStrategy;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RegionalRollupServiceTest {

    @Spy
    private RegionalVoteAccumulator accumulator =
            new RegionalVoteAccumulator(new SimpleCountStrategy());

    @Mock
    private RegionalResultPublisher publisher;

    @InjectMocks
    private RegionalRollupService service;

    private RawVoteEvent anyEvent(String region) {
        return new RawVoteEvent(
                UUID.randomUUID().toString(),
                UUID.randomUUID().toString(),
                UUID.randomUUID().toString(),
                region, "10.0.0.1", Instant.now()
        );
    }

    @Test
    void publishAllRegionsPublicaPorCadaRegionActiva() {
        // Arrange
        service.processVote(anyEvent("norte"));
        service.processVote(anyEvent("sur"));

        // Act
        service.publishAllRegions();

        // Assert: se publica una vez por región (2)
        verify(publisher, times(2)).publish(any());
    }

    @Test
    void sinVotosNoSePublicaNada() {
        // Act
        service.publishAllRegions();

        // Assert
        verify(publisher, never()).publish(any());
    }

    @Test
    void processVoteDelegaAlAcumulador() {
        // Arrange
        RawVoteEvent event = anyEvent("norte");

        // Act
        service.processVote(event);

        // Assert
        verify(accumulator, times(1)).accept(event);
    }
}

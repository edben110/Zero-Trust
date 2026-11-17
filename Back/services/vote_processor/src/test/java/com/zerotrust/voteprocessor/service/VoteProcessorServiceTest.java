package com.zerotrust.voteprocessor.service;

import com.zerotrust.voteprocessor.domain.model.CandidateId;
import com.zerotrust.voteprocessor.domain.model.RawVoteEvent;
import com.zerotrust.voteprocessor.domain.model.VoteAggregate;
import com.zerotrust.voteprocessor.domain.model.VoteId;
import com.zerotrust.voteprocessor.domain.model.VoterId;
import com.zerotrust.voteprocessor.domain.port.GlobalResultPublisher;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Pruebas de orquestación con Mockito. Sin Spring, sin brokers.
 */
@ExtendWith(MockitoExtension.class)
class VoteProcessorServiceTest {

    @Spy
    private VoteAggregate aggregate = new VoteAggregate();

    @Mock
    private GlobalResultPublisher publisher;

    @InjectMocks
    private VoteProcessorService service;

    private RawVoteEvent anyEvent() {
        return new RawVoteEvent(
                new VoteId(UUID.randomUUID().toString()),
                new VoterId(UUID.randomUUID().toString()),
                new CandidateId(UUID.randomUUID().toString()),
                "sur",
                "192.168.1.1",
                Instant.now()
        );
    }

    @Test
    void processVoteDelegaAlAgregado() {
        // Arrange
        RawVoteEvent event = anyEvent();

        // Act
        service.processVote(event);

        // Assert: el agregado recibió el voto
        verify(aggregate, times(1)).accept(event);
    }

    @Test
    void publishSnapshotPublicaCuandoHayVotos() {
        // Arrange
        service.processVote(anyEvent());

        // Act
        service.publishSnapshot();

        // Assert: el publisher fue llamado exactamente una vez
        verify(publisher, times(1)).publish(any());
    }

    @Test
    void publishSnapshotNOPublicaSinVotos() {
        // Arrange: agregado vacío (no se agrega ningún voto)

        // Act
        service.publishSnapshot();

        // Assert: no se publica nada
        verify(publisher, never()).publish(any());
    }
}

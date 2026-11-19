package com.zerotrust.voteprocessor.service;

import com.zerotrust.voteprocessor.domain.model.CandidateId;
import com.zerotrust.voteprocessor.domain.model.CandidateResult;
import com.zerotrust.voteprocessor.domain.model.GlobalResultSnapshot;
import com.zerotrust.voteprocessor.domain.model.RawVoteEvent;
import com.zerotrust.voteprocessor.domain.model.VoteId;
import com.zerotrust.voteprocessor.domain.model.VoterId;
import com.zerotrust.voteprocessor.domain.port.ConteoVotos;
import com.zerotrust.voteprocessor.domain.port.GlobalResultPublisher;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VoteProcessorServiceTest {

    private static final String C1 = "11111111-1111-1111-1111-111111111111";

    @Mock
    private ConteoVotos conteo;

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
    @DisplayName("Debe registrar el voto consumido en el conteo")
    void processVoteRegistraEnElConteo() {
        // Arrange
        RawVoteEvent event = anyEvent();

        // Act
        service.processVote(event);

        // Assert
        verify(conteo, times(1)).registrar(event);
        verifyNoInteractions(publisher);
    }

    @Test
    @DisplayName("Debe publicar el conteo actual como snapshot global")
    void publishSnapshotPublicaElConteoActual() {
        // Arrange
        CandidateResult resultado = new CandidateResult(new CandidateId(C1), C1, 7);
        when(conteo.conteoActual()).thenReturn(List.of(resultado));
        ArgumentCaptor<GlobalResultSnapshot> captor = ArgumentCaptor.forClass(GlobalResultSnapshot.class);

        // Act
        service.publishSnapshot();

        // Assert
        verify(publisher, times(1)).publish(captor.capture());
        assertEquals(List.of(resultado), captor.getValue().results());
        verifyNoMoreInteractions(publisher);
    }

    @Test
    @DisplayName("Debe publicar un snapshot vacío para limpiar los tableros tras un reinicio")
    void publishSnapshotPublicaVacioSinVotos() {
        // Arrange
        when(conteo.conteoActual()).thenReturn(List.of());
        ArgumentCaptor<GlobalResultSnapshot> captor = ArgumentCaptor.forClass(GlobalResultSnapshot.class);

        // Act
        service.publishSnapshot();

        // Assert
        verify(publisher, times(1)).publish(captor.capture());
        assertTrue(captor.getValue().results().isEmpty());
    }
}

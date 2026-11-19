package com.zerotrust.regionalrollup.service;

import com.zerotrust.regionalrollup.domain.model.RawVoteEvent;
import com.zerotrust.regionalrollup.domain.model.RegionalCandidateResult;
import com.zerotrust.regionalrollup.domain.model.RegionalResultSnapshot;
import com.zerotrust.regionalrollup.domain.port.ConteoVotosRegionales;
import com.zerotrust.regionalrollup.domain.port.RegionalResultPublisher;
import com.zerotrust.regionalrollup.domain.strategy.RegionalConsolidationStrategy;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RegionalRollupServiceTest {

    @Mock
    private ConteoVotosRegionales conteo;

    @Mock
    private RegionalConsolidationStrategy estrategia;

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
    @DisplayName("Debe registrar el voto consumido en el conteo regional")
    void processVoteRegistraEnElConteo() {
        // Arrange
        RawVoteEvent event = anyEvent("norte");

        // Act
        service.processVote(event);

        // Assert
        verify(conteo, times(1)).registrar(event);
        verifyNoInteractions(publisher, estrategia);
    }

    @Test
    @DisplayName("Debe consolidar con la estrategia cada región y publicar su resultado")
    void publicaCadaRegionConsolidadaPorLaEstrategia() {
        // Arrange
        Map<String, Long> votosNorte = Map.of("c1", 3L);
        RegionalCandidateResult consolidado = new RegionalCandidateResult("c1", "c1", 3);
        when(conteo.conteoPorRegion()).thenReturn(Map.of("norte", votosNorte));
        when(estrategia.consolidate("norte", votosNorte)).thenReturn(List.of(consolidado));
        ArgumentCaptor<RegionalResultSnapshot> captor = ArgumentCaptor.forClass(RegionalResultSnapshot.class);

        // Act
        service.publishAllRegions();

        // Assert
        verify(estrategia, times(1)).consolidate("norte", votosNorte);
        verify(publisher, times(1)).publish(captor.capture());
        assertEquals("norte", captor.getValue().region());
        assertEquals(List.of(consolidado), captor.getValue().results());
    }

    @Test
    @DisplayName("Debe publicar un resultado vacío para una región sin votos en la jornada activa")
    void publicaRegionSinVotosVacia() {
        // Arrange
        when(conteo.conteoPorRegion()).thenReturn(Map.of("sur", Map.of()));
        when(estrategia.consolidate("sur", Map.of())).thenReturn(List.of());
        ArgumentCaptor<RegionalResultSnapshot> captor = ArgumentCaptor.forClass(RegionalResultSnapshot.class);

        // Act
        service.publishAllRegions();

        // Assert
        verify(publisher, times(1)).publish(captor.capture());
        assertTrue(captor.getValue().results().isEmpty());
    }

    @Test
    @DisplayName("No debe publicar nada si no hay regiones")
    void sinRegionesNoPublica() {
        // Arrange
        when(conteo.conteoPorRegion()).thenReturn(Map.of());

        // Act
        service.publishAllRegions();

        // Assert
        verify(publisher, never()).publish(any());
    }
}

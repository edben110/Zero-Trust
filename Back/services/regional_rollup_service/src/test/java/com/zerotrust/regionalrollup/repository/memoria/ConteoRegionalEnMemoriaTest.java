package com.zerotrust.regionalrollup.repository.memoria;

import com.zerotrust.regionalrollup.domain.RawVoteEvent;
import com.zerotrust.regionalrollup.domain.RegionalVoteAccumulator;
import com.zerotrust.regionalrollup.domain.strategy.SimpleCountStrategy;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ConteoRegionalEnMemoriaTest {

    @Test
    @DisplayName("Debe devolver los votos registrados por región y candidato")
    void devuelveConteoPorRegion() {
        // Arrange
        ConteoRegionalEnMemoria conteo = new ConteoRegionalEnMemoria(new RegionalVoteAccumulator(new SimpleCountStrategy()));
        String candidato = UUID.randomUUID().toString();

        // Act
        conteo.registrar(new RawVoteEvent(UUID.randomUUID().toString(), UUID.randomUUID().toString(),
                candidato, "norte", "10.0.0.1", Instant.now()));
        Map<String, Map<String, Long>> resultado = conteo.conteoPorRegion();

        // Assert
        assertEquals(Map.of("norte", Map.of(candidato, 1L)), resultado);
        assertThrows(UnsupportedOperationException.class, () -> resultado.get("norte").put("otro", 1L));
    }
}

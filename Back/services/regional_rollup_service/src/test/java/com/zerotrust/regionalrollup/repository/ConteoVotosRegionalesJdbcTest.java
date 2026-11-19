package com.zerotrust.regionalrollup.repository;

import com.zerotrust.regionalrollup.domain.RawVoteEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowCallbackHandler;

import java.sql.ResultSet;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConteoVotosRegionalesJdbcTest {

    @Mock
    private JdbcTemplate jdbc;

    @InjectMocks
    private ConteoVotosRegionalesJdbc conteo;

    private static ResultSet fila(String region, String candidato, long total) throws Exception {
        ResultSet rs = mock(ResultSet.class);
        when(rs.getString("region")).thenReturn(region);
        when(rs.getString("candidate_id")).thenReturn(candidato);
        when(rs.getLong("total")).thenReturn(total);
        return rs;
    }

    @Test
    @DisplayName("Debe agrupar el conteo de la jornada activa por región y candidato")
    void agrupaPorRegionYCandidato() throws Exception {
        // Arrange
        when(jdbc.queryForList(ConteoVotosRegionalesJdbc.REGIONES, String.class)).thenReturn(List.of("norte"));
        List<ResultSet> filas = List.of(fila("norte", "c1", 3), fila("norte", "c2", 1));
        doAnswer(inv -> {
            RowCallbackHandler handler = inv.getArgument(1);
            for (ResultSet rs : filas) {
                handler.processRow(rs);
            }
            return null;
        }).when(jdbc).query(eq(ConteoVotosRegionalesJdbc.CONTEO_JORNADA_ACTIVA), any(RowCallbackHandler.class));

        // Act
        Map<String, Map<String, Long>> resultado = conteo.conteoPorRegion();

        // Assert
        assertEquals(Map.of("norte", Map.of("c1", 3L, "c2", 1L)), resultado);
    }

    @Test
    @DisplayName("Debe incluir con conteo vacío las regiones de jornadas anteriores para limpiar sus pantallas")
    void incluyeRegionesSinVotosActuales() {
        // Arrange
        when(jdbc.queryForList(ConteoVotosRegionalesJdbc.REGIONES, String.class)).thenReturn(List.of("sur"));

        // Act
        Map<String, Map<String, Long>> resultado = conteo.conteoPorRegion();

        // Assert
        assertEquals(Map.of("sur", Map.of()), resultado);
        ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
        verify(jdbc).query(sql.capture(), any(RowCallbackHandler.class));
        assertEquals(ConteoVotosRegionalesJdbc.CONTEO_JORNADA_ACTIVA, sql.getValue());
    }

    @Test
    @DisplayName("No debe tocar la base de datos al registrar un voto: voting_api ya lo persistió")
    void registrarNoTocaLaBaseDeDatos() {
        // Arrange
        RawVoteEvent voto = new RawVoteEvent(UUID.randomUUID().toString(), UUID.randomUUID().toString(),
                UUID.randomUUID().toString(), "norte", "10.0.0.1", Instant.now());

        // Act
        conteo.registrar(voto);

        // Assert
        verifyNoInteractions(jdbc);
    }
}

package com.zerotrust.voteprocessor.repository;

import com.zerotrust.voteprocessor.domain.CandidateId;
import com.zerotrust.voteprocessor.domain.CandidateResult;
import com.zerotrust.voteprocessor.domain.RawVoteEvent;
import com.zerotrust.voteprocessor.domain.VoteId;
import com.zerotrust.voteprocessor.domain.VoterId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConteoVotosJdbcTest {

    private static final String C1 = "11111111-1111-1111-1111-111111111111";
    private static final String C9 = "99999999-9999-9999-9999-999999999999";

    @Mock
    private JdbcTemplate jdbc;

    @InjectMocks
    private ConteoVotosJdbc conteo;

    @Test
    @DisplayName("Debe consultar los votos aceptados de la jornada activa agrupados por candidato")
    @SuppressWarnings("unchecked")
    void consultaLaJornadaActiva() throws Exception {
        // Arrange
        CandidateResult esperado = new CandidateResult(new CandidateId(C1), C1, 3);
        when(jdbc.query(eq(ConteoVotosJdbc.CONTEO_JORNADA_ACTIVA), any(RowMapper.class))).thenReturn(List.of(esperado));

        // Act
        List<CandidateResult> resultado = conteo.conteoActual();

        // Assert
        assertEquals(List.of(esperado), resultado);
        assertTrue(ConteoVotosJdbc.CONTEO_JORNADA_ACTIVA.contains("accepted_votes"));
        assertTrue(ConteoVotosJdbc.CONTEO_JORNADA_ACTIVA.contains("election_state"));
    }

    @Test
    @DisplayName("Debe convertir cada fila en un resultado de candidato")
    @SuppressWarnings("unchecked")
    void mapeaLasFilas() throws Exception {
        // Arrange
        ArgumentCaptor<RowMapper<CandidateResult>> captor = ArgumentCaptor.forClass(RowMapper.class);
        when(jdbc.query(eq(ConteoVotosJdbc.CONTEO_JORNADA_ACTIVA), captor.capture())).thenReturn(List.of());
        ResultSet fila = mock(ResultSet.class);
        when(fila.getString("candidate_id")).thenReturn(C9);
        when(fila.getLong("total")).thenReturn(12L);

        // Act
        conteo.conteoActual();
        CandidateResult mapeado = captor.getValue().mapRow(fila, 0);

        // Assert
        assertEquals(new CandidateResult(new CandidateId(C9), C9, 12), mapeado);
    }

    @Test
    @DisplayName("No debe tocar la base de datos al registrar un voto: voting_api ya lo persistió")
    void registrarNoTocaLaBaseDeDatos() {
        // Arrange
        RawVoteEvent voto = new RawVoteEvent(new VoteId(UUID.randomUUID().toString()),
                new VoterId(UUID.randomUUID().toString()), new CandidateId(UUID.randomUUID().toString()),
                "norte", "10.0.0.1", Instant.now());

        // Act
        conteo.registrar(voto);

        // Assert
        verifyNoInteractions(jdbc);
    }
}

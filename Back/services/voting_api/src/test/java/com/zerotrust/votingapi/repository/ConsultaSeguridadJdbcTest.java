package com.zerotrust.votingapi.repository;

import com.zerotrust.votingapi.exception.CanalSeguridadNoDisponibleException;
import com.zerotrust.votingapi.domain.EstadoIp;
import com.zerotrust.votingapi.domain.IncidenteSeguridad;
import com.zerotrust.votingapi.domain.ResultadoPurga;
import com.zerotrust.votingapi.domain.ResumenSeguridad;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConsultaSeguridadJdbcTest {

    private static final Instant VISTO = Instant.parse("2026-11-18T12:00:00Z");

    @Mock
    private JdbcTemplate jdbc;

    @InjectMocks
    private ConsultaSeguridadJdbc consulta;

    @Test
    @DisplayName("Debe borrar incidentes y estados y devolver cuántas filas borró")
    void purga() {
        // Arrange
        when(jdbc.update(ConsultaSeguridadJdbc.BORRAR_INCIDENTES)).thenReturn(4);
        when(jdbc.update(ConsultaSeguridadJdbc.BORRAR_ESTADOS)).thenReturn(2);

        // Act
        ResultadoPurga resultado = consulta.purgar();

        // Assert
        assertEquals(new ResultadoPurga(4, 2), resultado);
    }

    @Test
    @DisplayName("Debe convertir cada fila de ip_risk_state en un EstadoIp")
    @SuppressWarnings("unchecked")
    void mapeaEstados() throws Exception {
        // Arrange
        ArgumentCaptor<RowMapper<EstadoIp>> mapper = ArgumentCaptor.forClass(RowMapper.class);
        when(jdbc.query(eq(ConsultaSeguridadJdbc.ESTADOS), mapper.capture(), eq(100))).thenReturn(List.of());
        ResultSet fila = mock(ResultSet.class);
        when(fila.getString("ip")).thenReturn("1.1.1.1");
        when(fila.getString("risk_level")).thenReturn("BLOQUEADA");
        when(fila.getInt("vote_count")).thenReturn(11);
        when(fila.getInt("window_seconds")).thenReturn(60);
        when(fila.getTimestamp("first_seen")).thenReturn(Timestamp.from(VISTO));
        when(fila.getTimestamp("last_seen")).thenReturn(null);

        // Act
        consulta.estados(100);
        EstadoIp estado = mapper.getValue().mapRow(fila, 0);

        // Assert
        assertEquals(new EstadoIp("1.1.1.1", "BLOQUEADA", 11, 60, VISTO, null), estado);
    }

    @Test
    @DisplayName("Debe convertir cada fila de fraud_incidents en un IncidenteSeguridad")
    @SuppressWarnings("unchecked")
    void mapeaIncidentes() throws Exception {
        // Arrange
        ArgumentCaptor<RowMapper<IncidenteSeguridad>> mapper = ArgumentCaptor.forClass(RowMapper.class);
        when(jdbc.query(eq(ConsultaSeguridadJdbc.INCIDENTES), mapper.capture(), eq(200))).thenReturn(List.of());
        ResultSet fila = mock(ResultSet.class);
        when(fila.getLong("id")).thenReturn(9L);
        when(fila.getString("ip")).thenReturn("1.1.1.1");
        when(fila.getString("kind")).thenReturn("DUPLICATE_VOTE");
        when(fila.getString("risk_level")).thenReturn("NORMAL");
        when(fila.getInt("vote_count")).thenReturn(1);
        when(fila.getInt("window_seconds")).thenReturn(60);
        when(fila.getString("origin")).thenReturn("LIVE");
        when(fila.getTimestamp("detected_at")).thenReturn(Timestamp.from(VISTO));

        // Act
        consulta.incidentesRecientes(200);
        IncidenteSeguridad incidente = mapper.getValue().mapRow(fila, 0);

        // Assert
        assertEquals(new IncidenteSeguridad(9, "1.1.1.1", "DUPLICATE_VOTE", "NORMAL", 1, 60, "LIVE", VISTO), incidente);
    }

    @Test
    @DisplayName("Debe resumir los incidentes y contar las IPs por nivel con los tres niveles siempre presentes")
    @SuppressWarnings("unchecked")
    void resume() throws Exception {
        // Arrange
        ResultSet totales = mock(ResultSet.class);
        when(totales.getLong("total")).thenReturn(7L);
        when(totales.getLong("live")).thenReturn(5L);
        when(totales.getLong("drills")).thenReturn(2L);
        when(totales.getLong("duplicados")).thenReturn(1L);
        when(totales.getLong("region")).thenReturn(1L);
        when(totales.getLong("ips")).thenReturn(4L);
        when(jdbc.queryForObject(eq(ConsultaSeguridadJdbc.RESUMEN), any(RowMapper.class)))
                .thenAnswer(inv -> ((RowMapper<?>) inv.getArgument(1)).mapRow(totales, 0));
        ResultSet nivel = mock(ResultSet.class);
        when(nivel.getString("risk_level")).thenReturn("SOSPECHOSA");
        when(nivel.getLong("total")).thenReturn(3L);
        doAnswer(inv -> {
            ((RowCallbackHandler) inv.getArgument(1)).processRow(nivel);
            return null;
        }).when(jdbc).query(eq(ConsultaSeguridadJdbc.RIESGO_POR_NIVEL), any(RowCallbackHandler.class));

        // Act
        ResumenSeguridad resumen = consulta.resumen();

        // Assert
        assertEquals(new ResumenSeguridad(7, 5, 2, 1, 1, 4,
                Map.of("NORMAL", 0L, "SOSPECHOSA", 3L, "BLOQUEADA", 0L)), resumen);
        assertEquals(List.of("NORMAL", "SOSPECHOSA", "BLOQUEADA"), List.copyOf(resumen.riesgoActual().keySet()));
    }

    @Test
    @DisplayName("Debe traducir la caída de PostgreSQL a CanalSeguridadNoDisponibleException")
    void traduceElFallo() {
        // Arrange
        when(jdbc.update(ConsultaSeguridadJdbc.BORRAR_INCIDENTES))
                .thenThrow(new DataAccessResourceFailureException("sin conexión"));

        // Act + Assert
        assertThrows(CanalSeguridadNoDisponibleException.class, () -> consulta.purgar());
    }
}

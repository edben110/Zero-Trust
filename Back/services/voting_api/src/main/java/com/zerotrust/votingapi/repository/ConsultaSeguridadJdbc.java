package com.zerotrust.votingapi.repository;

import com.zerotrust.votingapi.exception.CanalSeguridadNoDisponibleException;
import com.zerotrust.votingapi.domain.EstadoIp;
import com.zerotrust.votingapi.domain.IncidenteSeguridad;
import com.zerotrust.votingapi.domain.ResultadoPurga;
import com.zerotrust.votingapi.domain.ResumenSeguridad;
import com.zerotrust.votingapi.port.ConsultaSeguridad;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowCallbackHandler;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Lee y purga las tablas que escribe bot_detector_service.
 */
public class ConsultaSeguridadJdbc implements ConsultaSeguridad {

    static final String RESUMEN = """
            SELECT count(*)                                          AS total,
                   count(*) FILTER (WHERE origin = 'LIVE')           AS live,
                   count(*) FILTER (WHERE origin = 'DRILL')          AS drills,
                   count(*) FILTER (WHERE kind = 'DUPLICATE_VOTE')   AS duplicados,
                   count(*) FILTER (WHERE kind = 'WRONG_REGION')     AS region,
                   count(DISTINCT ip)                                AS ips
            FROM fraud_incidents
            """;
    static final String RIESGO_POR_NIVEL =
            "SELECT risk_level, count(*) AS total FROM ip_risk_state GROUP BY risk_level";
    static final String ESTADOS = """
            SELECT ip, risk_level, vote_count, window_seconds, first_seen, last_seen
            FROM ip_risk_state
            ORDER BY last_seen DESC
            LIMIT ?
            """;
    static final String INCIDENTES = """
            SELECT id, ip, kind, risk_level, vote_count, window_seconds, origin, detected_at
            FROM fraud_incidents
            ORDER BY detected_at DESC, id DESC
            LIMIT ?
            """;
    static final String BORRAR_INCIDENTES = "DELETE FROM fraud_incidents";
    static final String BORRAR_ESTADOS = "DELETE FROM ip_risk_state";

    private static final List<String> NIVELES = List.of("NORMAL", "SOSPECHOSA", "BLOQUEADA");

    private final JdbcTemplate jdbc;

    public ConsultaSeguridadJdbc(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public ResumenSeguridad resumen() {
        return protegido(() -> {
            Map<String, Long> porNivel = new LinkedHashMap<>();
            NIVELES.forEach(nivel -> porNivel.put(nivel, 0L));
            jdbc.query(RIESGO_POR_NIVEL, (RowCallbackHandler) rs -> porNivel.put(rs.getString("risk_level"), rs.getLong("total")));
            return jdbc.queryForObject(RESUMEN, (rs, fila) -> new ResumenSeguridad(
                    rs.getLong("total"), rs.getLong("live"), rs.getLong("drills"),
                    rs.getLong("duplicados"), rs.getLong("region"), rs.getLong("ips"), porNivel));
        });
    }

    @Override
    public List<EstadoIp> estados(int limite) {
        return protegido(() -> jdbc.query(ESTADOS, (rs, fila) -> new EstadoIp(
                rs.getString("ip"), rs.getString("risk_level"), rs.getInt("vote_count"),
                rs.getInt("window_seconds"), instante(rs, "first_seen"), instante(rs, "last_seen")), limite));
    }

    @Override
    public List<IncidenteSeguridad> incidentesRecientes(int limite) {
        return protegido(() -> jdbc.query(INCIDENTES, (rs, fila) -> new IncidenteSeguridad(
                rs.getLong("id"), rs.getString("ip"), rs.getString("kind"), rs.getString("risk_level"),
                rs.getInt("vote_count"), rs.getInt("window_seconds"), rs.getString("origin"),
                instante(rs, "detected_at")), limite));
    }

    @Override
    public ResultadoPurga purgar() {
        return protegido(() -> new ResultadoPurga(jdbc.update(BORRAR_INCIDENTES), jdbc.update(BORRAR_ESTADOS)));
    }

    private static Instant instante(ResultSet rs, String columna) throws SQLException {
        Timestamp marca = rs.getTimestamp(columna);
        return marca == null ? null : marca.toInstant();
    }

    private static <T> T protegido(Supplier<T> consulta) {
        try {
            return consulta.get();
        } catch (DataAccessException e) {
            throw new CanalSeguridadNoDisponibleException("PostgreSQL no respondió", e);
        }
    }
}

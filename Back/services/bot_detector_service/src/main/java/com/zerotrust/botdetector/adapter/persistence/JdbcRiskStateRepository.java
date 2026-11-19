package com.zerotrust.botdetector.adapter.persistence;

import com.zerotrust.botdetector.exception.ProyeccionNoDisponibleException;
import com.zerotrust.botdetector.domain.port.RiskStateRepository;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.Instant;

/**
 * Adaptador JDBC de la proyección de estado. Usa UPSERT para no crecer con una
 * fila por voto: solo interesa el estado vigente de cada IP.
 */
@Component
public class JdbcRiskStateRepository implements RiskStateRepository {

    static final String UPSERT = """
            INSERT INTO ip_risk_state (ip, risk_level, vote_count, window_seconds, first_seen, last_seen)
            VALUES (?, ?, ?, ?, ?, ?)
            ON CONFLICT (ip) DO UPDATE SET
                risk_level     = EXCLUDED.risk_level,
                vote_count     = EXCLUDED.vote_count,
                window_seconds = EXCLUDED.window_seconds,
                last_seen      = EXCLUDED.last_seen
            """;

    private final JdbcTemplate jdbc;

    public JdbcRiskStateRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    @Transactional
    public void actualizar(String ip, String riskLevel, int voteCount, int windowSeconds, Instant seenAt) {
        try {
            jdbc.update(UPSERT, ip, riskLevel, voteCount, windowSeconds,
                    Timestamp.from(seenAt), Timestamp.from(seenAt));
        } catch (DataAccessException e) {
            throw new ProyeccionNoDisponibleException("No se pudo actualizar ip_risk_state", e);
        }
    }
}

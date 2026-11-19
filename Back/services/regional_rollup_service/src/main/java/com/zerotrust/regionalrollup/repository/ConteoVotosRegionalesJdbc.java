package com.zerotrust.regionalrollup.repository;

import com.zerotrust.regionalrollup.domain.RawVoteEvent;
import com.zerotrust.regionalrollup.port.ConteoVotosRegionales;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowCallbackHandler;

import java.util.HashMap;
import java.util.Map;

// PostgreSQL es la fuente de verdad del escrutinio.
public class ConteoVotosRegionalesJdbc implements ConteoVotosRegionales {

    // Incluye regiones de jornadas anteriores para enviarles un snapshot vacío tras el reinicio.
    static final String REGIONES = "SELECT DISTINCT region FROM accepted_votes";

    static final String CONTEO_JORNADA_ACTIVA = "SELECT v.region, v.candidate_id, COUNT(*) AS total "
            + "FROM accepted_votes v JOIN election_state e ON e.run_id=v.run_id "
            + "WHERE e.id=1 GROUP BY v.region, v.candidate_id";

    private final JdbcTemplate jdbc;

    public ConteoVotosRegionalesJdbc(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void registrar(RawVoteEvent voto) {
        // voting_api ya insertó el voto en accepted_votes; contarlo aquí lo duplicaría.
    }

    @Override
    public Map<String, Map<String, Long>> conteoPorRegion() {
        Map<String, Map<String, Long>> conteo = new HashMap<>();
        for (String region : jdbc.queryForList(REGIONES, String.class)) {
            conteo.put(region, new HashMap<>());
        }
        jdbc.query(CONTEO_JORNADA_ACTIVA, (RowCallbackHandler) rs -> conteo
                .computeIfAbsent(rs.getString("region"), region -> new HashMap<>())
                .put(rs.getString("candidate_id"), rs.getLong("total")));
        Map<String, Map<String, Long>> copia = new HashMap<>();
        conteo.forEach((region, votos) -> copia.put(region, Map.copyOf(votos)));
        return Map.copyOf(copia);
    }
}

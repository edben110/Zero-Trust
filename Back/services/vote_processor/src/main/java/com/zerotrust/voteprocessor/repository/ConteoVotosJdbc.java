package com.zerotrust.voteprocessor.repository;

import com.zerotrust.voteprocessor.domain.CandidateId;
import com.zerotrust.voteprocessor.domain.CandidateResult;
import com.zerotrust.voteprocessor.domain.RawVoteEvent;
import com.zerotrust.voteprocessor.port.ConteoVotos;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;

/**
 * Conteo global leído de PostgreSQL, fuente de verdad del escrutinio.
 */
public class ConteoVotosJdbc implements ConteoVotos {

    static final String CONTEO_JORNADA_ACTIVA = "SELECT v.candidate_id, COUNT(*) AS total "
            + "FROM accepted_votes v JOIN election_state e ON e.run_id=v.run_id "
            + "WHERE e.id=1 GROUP BY v.candidate_id";

    private final JdbcTemplate jdbc;

    public ConteoVotosJdbc(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void registrar(RawVoteEvent voto) {
        // voting_api ya insertó el voto en accepted_votes; contarlo aquí lo duplicaría.
    }

    @Override
    public List<CandidateResult> conteoActual() {
        return jdbc.query(CONTEO_JORNADA_ACTIVA, (rs, fila) -> new CandidateResult(
                new CandidateId(rs.getString("candidate_id")),
                rs.getString("candidate_id"),
                rs.getLong("total")));
    }
}

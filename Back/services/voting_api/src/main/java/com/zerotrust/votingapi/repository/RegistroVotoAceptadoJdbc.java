package com.zerotrust.votingapi.repository;

import com.zerotrust.votingapi.domain.VotoEmitido;
import com.zerotrust.votingapi.port.RegistroVotoAceptado;
import org.springframework.jdbc.core.JdbcTemplate;

public final class RegistroVotoAceptadoJdbc implements RegistroVotoAceptado {
    private final JdbcTemplate jdbc;

    public RegistroVotoAceptadoJdbc(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void registrar(VotoEmitido voto) {
        int filas = jdbc.update("INSERT INTO accepted_votes "
                + "(run_id, vote_id, voter_uuid, candidate_id, region, voted_at) "
                + "SELECT run_id, ?, ?, ?, ?, ? FROM election_state WHERE id=1",
                voto.voteId().valor(), voto.voterId().valor(), voto.candidateId().valor(),
                voto.region().valor(), java.sql.Timestamp.from(voto.timestamp()));
        if (filas != 1) throw new IllegalStateException("No existe elección activa");
    }
}

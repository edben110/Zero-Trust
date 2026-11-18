package com.zerotrust.votingapi.adapter.persistence;

import com.zerotrust.votingapi.domain.ElectionId;
import com.zerotrust.votingapi.domain.Region;
import com.zerotrust.votingapi.domain.VotanteNoHabilitadoException;
import com.zerotrust.votingapi.domain.VoterId;
import com.zerotrust.votingapi.domain.VotoDuplicadoException;
import com.zerotrust.votingapi.port.ControlRegionVotante;
import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;

public final class ControlRegionVotanteJdbc implements ControlRegionVotante {
    private final JdbcTemplate jdbc;

    public ControlRegionVotanteJdbc(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public Resultado evaluar(ElectionId eleccion, VoterId votante, Region seleccionada) {
        // FOR SHARE serializa la jornada con el reset; FOR UPDATE serializa
        // todos los intentos concurrentes del mismo votante.
        UUID jornada = jdbc.queryForObject(
                "SELECT run_id FROM election_state WHERE id=1 FOR SHARE", UUID.class);
        if (jornada == null) throw new IllegalStateException("No existe elección activa");
        List<String> regiones = jdbc.query(
                "SELECT region FROM registered_voters WHERE voter_id=? FOR UPDATE",
                (rs, fila) -> rs.getString(1), votante.valor());
        if (regiones.isEmpty()) throw new VotanteNoHabilitadoException();

        Boolean yaVoto = jdbc.queryForObject(
                "SELECT EXISTS(SELECT 1 FROM voter_election_usage WHERE election_id=? AND voter_uuid=?)",
                Boolean.class, eleccion.valor(), votante.valor());
        if (Boolean.TRUE.equals(yaVoto)) throw new VotoDuplicadoException();

        List<Integer> fallos = jdbc.query(
                "SELECT failures FROM voter_region_attempts WHERE run_id=? AND voter_uuid=?",
                (rs, fila) -> rs.getInt(1), jornada, votante.valor());
        if (!fallos.isEmpty() && fallos.get(0) >= 2) return Resultado.ANULADO;
        if (regiones.get(0).equals(seleccionada.valor())) return Resultado.CORRECTA;

        Integer actualizados = jdbc.queryForObject(
                "INSERT INTO voter_region_attempts (run_id, voter_uuid, failures) VALUES (?, ?, 1) "
                        + "ON CONFLICT (run_id, voter_uuid) DO UPDATE SET failures="
                        + "LEAST(2, voter_region_attempts.failures + 1) RETURNING failures",
                Integer.class, jornada, votante.valor());
        return actualizados != null && actualizados >= 2 ? Resultado.ANULADO : Resultado.PRIMER_ERROR;
    }
}

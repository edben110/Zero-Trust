package com.zerotrust.votingapi.adapter.persistence;

import com.zerotrust.votingapi.domain.ElectionId;
import com.zerotrust.votingapi.domain.VoterId;
import com.zerotrust.votingapi.domain.VotoDuplicadoException;
import com.zerotrust.votingapi.port.RegistroParticipacion;
import org.springframework.jdbc.core.JdbcTemplate;

// La unicidad la decide PostgreSQL con UNIQUE (election_id, voter_uuid) de docs/schema.sql.
// Con dos solicitudes simultáneas para el mismo UUID, la segunda espera a que la primera termine su transacción:
// si la primera confirma, ON CONFLICT no inserta nada (duplicado); si revierte, la segunda sí inserta.
public class RegistroParticipacionJdbc implements RegistroParticipacion {

    // El bloqueo compartido serializa el registro con la rotación del run_id.
    private static final String INSERTAR = "WITH actual AS (SELECT run_id FROM election_state WHERE id=1 FOR SHARE) "
            + "INSERT INTO voter_election_usage (election_id, voter_uuid, used_at) "
            + "SELECT ?, ?, now() FROM actual WHERE true "
            + "ON CONFLICT (election_id, voter_uuid) DO NOTHING";

    private final JdbcTemplate jdbc;

    public RegistroParticipacionJdbc(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void registrar(ElectionId eleccion, VoterId voterId) {
        int insertadas = jdbc.update(INSERTAR, eleccion.valor(), voterId.valor());
        if (insertadas == 0) {
            throw new VotoDuplicadoException();
        }
    }
}

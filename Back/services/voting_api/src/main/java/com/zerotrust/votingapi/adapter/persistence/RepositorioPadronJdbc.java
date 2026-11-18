package com.zerotrust.votingapi.adapter.persistence;

import com.zerotrust.votingapi.domain.VotanteInscrito;
import com.zerotrust.votingapi.domain.VotanteYaInscritoException;
import com.zerotrust.votingapi.port.RepositorioPadron;
import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.Timestamp;

// Misma tabla que llena voter_registration_service (docs/schema.sql). voter_id es UNIQUE: con ON CONFLICT
// un alta repetida no inserta nada y se informa como ya inscrito, incluso con dos altas simultáneas.
public class RepositorioPadronJdbc implements RepositorioPadron {

    private static final String INSERTAR = "INSERT INTO registered_voters (voter_id, region, registered_at) "
            + "VALUES (?, ?, ?) ON CONFLICT (voter_id) DO NOTHING";

    private final JdbcTemplate jdbc;

    public RepositorioPadronJdbc(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void inscribir(VotanteInscrito votante) {
        int insertadas = jdbc.update(INSERTAR, votante.voterId().valor(),
                votante.region().valor(), Timestamp.from(votante.inscritoEn()));
        if (insertadas == 0) {
            throw new VotanteYaInscritoException();
        }
    }
}

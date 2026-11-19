package com.zerotrust.votingapi.repository;

import com.zerotrust.votingapi.domain.ElectionId;
import com.zerotrust.votingapi.domain.EvaluacionRegion;
import com.zerotrust.votingapi.domain.Region;
import com.zerotrust.votingapi.domain.ResultadoRegion;
import com.zerotrust.votingapi.domain.VotanteEnJornada;
import com.zerotrust.votingapi.domain.VoterId;
import com.zerotrust.votingapi.exception.VotanteNoHabilitadoException;
import com.zerotrust.votingapi.port.ControlRegionVotante;
import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Carga y guarda el estado del votante en la jornada; las reglas viven en VotanteEnJornada.
 */
public final class ControlRegionVotanteJdbc implements ControlRegionVotante {

    // FOR SHARE serializa la jornada con el reset; FOR UPDATE serializa
    // todos los intentos concurrentes del mismo votante.
    static final String JORNADA_ACTIVA = "SELECT run_id FROM election_state WHERE id=1 FOR SHARE";
    static final String REGION_VOTANTE = "SELECT region FROM registered_voters WHERE voter_id=? FOR UPDATE";
    static final String YA_VOTO =
            "SELECT EXISTS(SELECT 1 FROM voter_election_usage WHERE election_id=? AND voter_uuid=?)";
    static final String FALLOS_REGION =
            "SELECT failures FROM voter_region_attempts WHERE run_id=? AND voter_uuid=?";
    static final String GUARDAR_FALLOS = "INSERT INTO voter_region_attempts (run_id, voter_uuid, failures) "
            + "VALUES (?, ?, ?) ON CONFLICT (run_id, voter_uuid) DO UPDATE SET failures=EXCLUDED.failures";

    private final JdbcTemplate jdbc;

    public ControlRegionVotanteJdbc(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public ResultadoRegion evaluar(ElectionId eleccion, VoterId votante, Region seleccionada) {
        UUID jornada = jdbc.queryForObject(JORNADA_ACTIVA, UUID.class);
        if (jornada == null) throw new IllegalStateException("No existe elección activa");
        List<String> regiones = jdbc.query(REGION_VOTANTE, (rs, fila) -> rs.getString(1), votante.valor());
        if (regiones.isEmpty()) throw new VotanteNoHabilitadoException();

        Boolean yaVoto = jdbc.queryForObject(YA_VOTO, Boolean.class, eleccion.valor(), votante.valor());
        List<Integer> fallos = jdbc.query(FALLOS_REGION, (rs, fila) -> rs.getInt(1), jornada, votante.valor());
        int fallosPrevios = fallos.isEmpty() ? 0 : fallos.get(0);

        VotanteEnJornada enJornada = new VotanteEnJornada(
                new Region(regiones.get(0)), Boolean.TRUE.equals(yaVoto), fallosPrevios);
        EvaluacionRegion evaluacion = enJornada.evaluarRegion(seleccionada);
        if (evaluacion.fallosCambiaron(fallosPrevios)) {
            jdbc.update(GUARDAR_FALLOS, jornada, votante.valor(), evaluacion.fallos());
        }
        return evaluacion.resultado();
    }
}

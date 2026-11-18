package com.zerotrust.regionalrollup.service;

import com.zerotrust.regionalrollup.domain.model.RawVoteEvent;
import com.zerotrust.regionalrollup.domain.model.RegionalCandidateResult;
import com.zerotrust.regionalrollup.domain.model.RegionalResultSnapshot;
import com.zerotrust.regionalrollup.domain.model.RegionalVoteAccumulator;
import com.zerotrust.regionalrollup.domain.port.RegionalResultPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import java.time.Instant;

@Service
public class RegionalRollupService {

    private final RegionalVoteAccumulator accumulator;
    private final RegionalResultPublisher  publisher;
    private final JdbcTemplate jdbc;

    public RegionalRollupService(RegionalVoteAccumulator accumulator,
                                  RegionalResultPublisher publisher) {
        this(accumulator, publisher, null);
    }

    @Autowired
    public RegionalRollupService(RegionalVoteAccumulator accumulator,
                                  RegionalResultPublisher publisher, JdbcTemplate jdbc) {
        this.accumulator = accumulator;
        this.publisher   = publisher;
        this.jdbc = jdbc;
    }

    public void processVote(RawVoteEvent event) {
        accumulator.accept(event);
    }

    @Scheduled(fixedDelayString = "${rollup.publish-interval-ms:5000}")
    public void publishAllRegions() {
        if (jdbc != null) {
            // Incluye regiones de jornadas anteriores para enviar un snapshot vacío
            // y limpiar sus pantallas después del reset.
            var regiones = jdbc.queryForList("SELECT DISTINCT region FROM accepted_votes", String.class);
            for (String region : regiones) {
                var results = jdbc.query("SELECT v.candidate_id, COUNT(*) AS total "
                                + "FROM accepted_votes v JOIN election_state e ON e.run_id=v.run_id "
                                + "WHERE e.id=1 AND v.region=? GROUP BY v.candidate_id",
                        (rs, row) -> new RegionalCandidateResult(rs.getString("candidate_id"),
                                rs.getString("candidate_id"), rs.getLong("total")), region);
                publisher.publish(new RegionalResultSnapshot(region, Instant.now(), results));
            }
            return;
        }
        for (String region : accumulator.activeRegions()) {
            publisher.publish(accumulator.snapshotFor(region));
        }
    }
}

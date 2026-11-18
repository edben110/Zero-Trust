package com.zerotrust.voteprocessor.service;

import com.zerotrust.voteprocessor.domain.model.GlobalResultSnapshot;
import com.zerotrust.voteprocessor.domain.model.CandidateResult;
import com.zerotrust.voteprocessor.domain.model.CandidateId;
import com.zerotrust.voteprocessor.domain.model.RawVoteEvent;
import com.zerotrust.voteprocessor.domain.model.VoteAggregate;
import com.zerotrust.voteprocessor.domain.port.GlobalResultPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import java.time.Instant;

/**
 * Orquesta la recepción de votos y la publicación periódica del snapshot global.
 * La lógica de negocio vive en VoteAggregate; este servicio solo coordina.
 */
@Service
public class VoteProcessorService {

    private final VoteAggregate      aggregate;
    private final GlobalResultPublisher publisher;
    private final JdbcTemplate jdbc;

    public VoteProcessorService(VoteAggregate aggregate, GlobalResultPublisher publisher) {
        this(aggregate, publisher, null);
    }

    @Autowired
    public VoteProcessorService(VoteAggregate aggregate, GlobalResultPublisher publisher, JdbcTemplate jdbc) {
        this.aggregate  = aggregate;
        this.publisher  = publisher;
        this.jdbc = jdbc;
    }

    /**
     * Punto de entrada para cada voto consumido desde raw_votes.
     */
    public void processVote(RawVoteEvent event) {
        aggregate.accept(event);
    }

    /**
     * Publica el snapshot global cada 5 segundos.
     * Separar el disparo de la lógica permite testear ambos sin timers.
     */
    @Scheduled(fixedDelayString = "${processor.publish-interval-ms:5000}")
    public void publishSnapshot() {
        if (jdbc != null) {
            var results = jdbc.query("SELECT v.candidate_id, COUNT(*) AS total "
                            + "FROM accepted_votes v JOIN election_state e ON e.run_id=v.run_id "
                            + "WHERE e.id=1 GROUP BY v.candidate_id",
                    (rs, row) -> new CandidateResult(new CandidateId(rs.getString("candidate_id")),
                            rs.getString("candidate_id"), rs.getLong("total")));
            // La lista vacía es un snapshot válido: limpia el resultado anterior tras un reinicio.
            publisher.publish(new GlobalResultSnapshot(Instant.now(), results));
            return;
        }
        GlobalResultSnapshot snapshot = aggregate.snapshot();
        if (!snapshot.results().isEmpty()) {
            publisher.publish(snapshot);
        }
    }
}

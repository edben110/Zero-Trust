package com.zerotrust.voteprocessor.service;

import com.zerotrust.voteprocessor.domain.model.GlobalResultSnapshot;
import com.zerotrust.voteprocessor.domain.model.RawVoteEvent;
import com.zerotrust.voteprocessor.domain.model.VoteAggregate;
import com.zerotrust.voteprocessor.domain.port.GlobalResultPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/**
 * Orquesta la recepción de votos y la publicación periódica del snapshot global.
 * La lógica de negocio vive en VoteAggregate; este servicio solo coordina.
 */
@Service
public class VoteProcessorService {

    private final VoteAggregate      aggregate;
    private final GlobalResultPublisher publisher;

    public VoteProcessorService(VoteAggregate aggregate, GlobalResultPublisher publisher) {
        this.aggregate  = aggregate;
        this.publisher  = publisher;
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
        GlobalResultSnapshot snapshot = aggregate.snapshot();
        if (!snapshot.results().isEmpty()) {
            publisher.publish(snapshot);
        }
    }
}

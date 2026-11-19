package com.zerotrust.voteprocessor.service;

import com.zerotrust.voteprocessor.domain.GlobalResultSnapshot;
import com.zerotrust.voteprocessor.domain.RawVoteEvent;
import com.zerotrust.voteprocessor.port.ConteoVotos;
import com.zerotrust.voteprocessor.port.GlobalResultPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
public class VoteProcessorService {

    private final ConteoVotos conteo;
    private final GlobalResultPublisher publisher;

    public VoteProcessorService(ConteoVotos conteo, GlobalResultPublisher publisher) {
        this.conteo = conteo;
        this.publisher = publisher;
    }

    public void processVote(RawVoteEvent event) {
        conteo.registrar(event);
    }

    @Scheduled(fixedDelayString = "${processor.publish-interval-ms:5000}")
    public void publishSnapshot() {
        // La lista vacía es un snapshot válido: limpia el resultado anterior tras un reinicio.
        publisher.publish(new GlobalResultSnapshot(Instant.now(), conteo.conteoActual()));
    }
}

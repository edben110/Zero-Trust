package com.zerotrust.regionalrollup.service;

import com.zerotrust.regionalrollup.domain.model.RawVoteEvent;
import com.zerotrust.regionalrollup.domain.model.RegionalResultSnapshot;
import com.zerotrust.regionalrollup.domain.port.ConteoVotosRegionales;
import com.zerotrust.regionalrollup.domain.port.RegionalResultPublisher;
import com.zerotrust.regionalrollup.domain.strategy.RegionalConsolidationStrategy;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
public class RegionalRollupService {

    private final ConteoVotosRegionales conteo;
    private final RegionalConsolidationStrategy estrategia;
    private final RegionalResultPublisher publisher;

    public RegionalRollupService(ConteoVotosRegionales conteo,
                                 RegionalConsolidationStrategy estrategia,
                                 RegionalResultPublisher publisher) {
        this.conteo = conteo;
        this.estrategia = estrategia;
        this.publisher = publisher;
    }

    public void processVote(RawVoteEvent event) {
        conteo.registrar(event);
    }

    @Scheduled(fixedDelayString = "${rollup.publish-interval-ms:5000}")
    public void publishAllRegions() {
        conteo.conteoPorRegion().forEach((region, votos) -> publisher.publish(
                new RegionalResultSnapshot(region, Instant.now(), estrategia.consolidate(region, votos))));
    }
}

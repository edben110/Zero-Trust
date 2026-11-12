package com.zerotrust.regionalrollup.service;

import com.zerotrust.regionalrollup.domain.model.RawVoteEvent;
import com.zerotrust.regionalrollup.domain.model.RegionalVoteAccumulator;
import com.zerotrust.regionalrollup.domain.port.RegionalResultPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class RegionalRollupService {

    private final RegionalVoteAccumulator accumulator;
    private final RegionalResultPublisher  publisher;

    public RegionalRollupService(RegionalVoteAccumulator accumulator,
                                 RegionalResultPublisher publisher) {
        this.accumulator = accumulator;
        this.publisher   = publisher;
    }

    public void processVote(RawVoteEvent event) {
        accumulator.accept(event);
    }

    @Scheduled(fixedDelayString = "${rollup.publish-interval-ms:5000}")
    public void publishAllRegions() {
        for (String region : accumulator.activeRegions()) {
            publisher.publish(accumulator.snapshotFor(region));
        }
    }
}

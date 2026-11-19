package com.zerotrust.regionalrollup.domain.strategy;

import com.zerotrust.regionalrollup.domain.RegionalCandidateResult;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public final class SimpleCountStrategy implements RegionalConsolidationStrategy {

    @Override
    public List<RegionalCandidateResult> consolidate(String region, Map<String, Long> rawCounts) {
        return rawCounts.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue(Comparator.reverseOrder()))
                .map(e -> new RegionalCandidateResult(e.getKey(), e.getKey(), e.getValue()))
                .collect(Collectors.toList());
    }
}

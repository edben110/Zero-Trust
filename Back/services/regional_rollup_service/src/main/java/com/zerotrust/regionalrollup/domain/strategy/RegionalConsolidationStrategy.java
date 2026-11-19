package com.zerotrust.regionalrollup.domain.strategy;

import com.zerotrust.regionalrollup.domain.RegionalCandidateResult;

import java.util.List;
import java.util.Map;

public interface RegionalConsolidationStrategy {

    List<RegionalCandidateResult> consolidate(String region, Map<String, Long> rawCounts);
}

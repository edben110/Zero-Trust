package com.zerotrust.votingapi.dto.response;

import java.util.List;
import java.util.Map;

public record PanelSeguridadResponse(Umbrales thresholds, Resumen summary, List<Estado> ips, List<Incidente> incidents) {

    public record Umbrales(int suspicious, int blocked, int windowSeconds) {
    }

    public record Resumen(long incidents, long live, long drills, long duplicates, long wrongRegion,
                          long ipsWithIncidents, Map<String, Long> currentRisk) {
    }

    public record Estado(String ip, String riskLevel, int voteCount, int windowSeconds,
                         String firstSeen, String lastSeen) {
    }

    public record Incidente(long id, String ip, String kind, String riskLevel, int voteCount,
                            int windowSeconds, String origin, String detectedAt) {
    }
}

package com.zerotrust.votingapi.dto.response;

public record PurgaRealizadaResponse(boolean purged, int incidents, int ips) {
}

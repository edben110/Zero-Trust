package com.zerotrust.votingapi.dto.response;

public record PruebaAceptadaResponse(boolean accepted, String ip, int votes, String topic) {
}

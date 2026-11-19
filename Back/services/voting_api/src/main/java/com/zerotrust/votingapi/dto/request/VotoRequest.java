package com.zerotrust.votingapi.dto.request;

public record VotoRequest(String voterId, String candidateId, String region) {
}

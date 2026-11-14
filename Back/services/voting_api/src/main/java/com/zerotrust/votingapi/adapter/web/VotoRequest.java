package com.zerotrust.votingapi.adapter.web;

public record VotoRequest(String voterId, String candidateId, String region) {
}

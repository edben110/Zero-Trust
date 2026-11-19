package com.zerotrust.voteprocessor.domain;

public record CandidateResult(CandidateId candidateId, String candidateName, long voteCount) {

    public CandidateResult {
        if (candidateName == null || candidateName.isBlank()) {
            throw new IllegalArgumentException("candidateName no puede ser nulo ni vacío");
        }
        if (voteCount < 0) {
            throw new IllegalArgumentException("voteCount no puede ser negativo");
        }
    }
}

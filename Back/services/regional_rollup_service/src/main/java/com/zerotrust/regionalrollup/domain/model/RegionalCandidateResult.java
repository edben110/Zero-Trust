package com.zerotrust.regionalrollup.domain.model;

public record RegionalCandidateResult(String candidateId, String candidateName, long voteCount) {

    public RegionalCandidateResult {
        if (candidateId == null || candidateId.isBlank()) {
            throw new IllegalArgumentException("candidateId no puede ser nulo ni vacío");
        }
        if (candidateName == null || candidateName.isBlank()) {
            throw new IllegalArgumentException("candidateName no puede ser nulo ni vacío");
        }
        if (voteCount < 0) {
            throw new IllegalArgumentException("voteCount no puede ser negativo");
        }
    }
}

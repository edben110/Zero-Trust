package com.zerotrust.voteprocessor.domain.model;

/**
 * Par candidato→conteo dentro de un resultado global o regional.
 */
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

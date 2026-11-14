package com.zerotrust.votingapi.domain;

public record CandidateId(String valor) {

    public CandidateId {
        valor = UuidCanonico.normalizar(valor, "candidateId");
    }
}

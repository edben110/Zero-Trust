package com.zerotrust.analyticsarchiver.domain;

public record CandidateId(String valor) {

    public CandidateId {
        valor = UuidCanonico.normalizar(valor, "candidateId");
    }
}

package com.zerotrust.analyticsarchiver.domain;

public record VoteId(String valor) {

    public VoteId {
        valor = UuidCanonico.normalizar(valor, "voteId");
    }
}

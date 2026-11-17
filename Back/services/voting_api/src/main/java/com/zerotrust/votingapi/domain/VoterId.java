package com.zerotrust.votingapi.domain;

public record VoterId(String valor) {

    public VoterId {
        valor = UuidCanonico.normalizar(valor, "voterId");
    }
}

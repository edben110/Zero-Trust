package com.zerotrust.votingapi.domain;

import java.util.UUID;

public record VoteId(String valor) {

    public VoteId {
        valor = UuidCanonico.normalizar(valor, "voteId");
    }

    public static VoteId de(UUID uuid) {
        return new VoteId(uuid.toString());
    }
}

package com.zerotrust.voteprocessor.domain;

import java.util.UUID;

public record VoteId(String value) {

    public VoteId {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("VoteId no puede ser nulo ni vacío");
        }
        UUID.fromString(value);
    }
}

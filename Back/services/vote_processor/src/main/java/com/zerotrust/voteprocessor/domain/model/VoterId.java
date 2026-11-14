package com.zerotrust.voteprocessor.domain.model;

import java.util.UUID;

public record VoterId(String value) {

    public VoterId {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("VoterId no puede ser nulo ni vacío");
        }
        UUID.fromString(value);
    }
}

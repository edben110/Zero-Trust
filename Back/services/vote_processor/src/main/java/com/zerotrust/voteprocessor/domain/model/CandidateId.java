package com.zerotrust.voteprocessor.domain.model;

import java.util.UUID;

public record CandidateId(String value) {

    public CandidateId {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("CandidateId no puede ser nulo ni vacío");
        }
        UUID.fromString(value);
    }
}

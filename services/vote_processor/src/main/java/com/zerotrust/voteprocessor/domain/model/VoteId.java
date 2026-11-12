package com.zerotrust.voteprocessor.domain.model;

import java.util.UUID;

/**
 * Identificador inmutable de un voto. Fail-Fast: rechaza valores nulos o vacíos
 * en el momento de construcción para que los invariantes del dominio nunca queden
 * en estado inválido.
 */
public record VoteId(String value) {

    public VoteId {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("VoteId no puede ser nulo ni vacío");
        }
        // Valida formato UUID
        UUID.fromString(value);
    }
}

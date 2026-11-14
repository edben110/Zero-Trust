package com.zerotrust.voterregistration.domain;

import java.time.Instant;
import java.util.Objects;

public record VotanteRegistrado(VoterId voterId, NombreCompleto nombreCompleto, Region region, Instant registradoEn) {

    public VotanteRegistrado {
        Objects.requireNonNull(voterId, "voterId obligatorio");
        Objects.requireNonNull(nombreCompleto, "fullName obligatorio");
        Objects.requireNonNull(region, "region obligatoria");
        Objects.requireNonNull(registradoEn, "registeredAt obligatorio");
    }
}

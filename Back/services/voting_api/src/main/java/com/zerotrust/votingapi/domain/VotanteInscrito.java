package com.zerotrust.votingapi.domain;

import java.time.Instant;
import java.util.Objects;

// Alta en el padrón hecha por el administrador. Solo lleva el UUID derivado: la cédula no sale del servicio.
public record VotanteInscrito(VoterId voterId, NombreCompleto nombre, Region region, Instant inscritoEn) {

    public VotanteInscrito {
        Objects.requireNonNull(voterId, "voterId obligatorio");
        Objects.requireNonNull(nombre, "fullName obligatorio");
        Objects.requireNonNull(region, "region obligatoria");
        Objects.requireNonNull(inscritoEn, "inscritoEn obligatorio");
    }
}

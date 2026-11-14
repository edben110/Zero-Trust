package com.zerotrust.votingapi.domain;

import java.util.Objects;

public record Candidato(CandidateId candidateId, String nombre, String lista) {

    public Candidato {
        Objects.requireNonNull(candidateId, "candidateId obligatorio");
        if (nombre == null || nombre.isBlank()) {
            throw new IllegalArgumentException("el candidato necesita nombre");
        }
        if (lista == null || lista.isBlank()) {
            throw new IllegalArgumentException("el candidato necesita lista");
        }
    }
}

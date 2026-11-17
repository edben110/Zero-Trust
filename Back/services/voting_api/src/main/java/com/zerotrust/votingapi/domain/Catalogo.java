package com.zerotrust.votingapi.domain;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public final class Catalogo {

    private final List<Candidato> candidatos;
    private final List<Region> regiones;
    private final Set<CandidateId> idsCandidatos;

    public Catalogo(List<Candidato> candidatos, List<Region> regiones) {
        this.candidatos = List.copyOf(candidatos);
        this.regiones = List.copyOf(regiones);
        if (this.candidatos.isEmpty()) {
            throw new IllegalArgumentException("el catálogo necesita al menos un candidato");
        }
        if (this.regiones.isEmpty()) {
            throw new IllegalArgumentException("el catálogo necesita al menos una región");
        }
        this.idsCandidatos = this.candidatos.stream().map(Candidato::candidateId).collect(Collectors.toUnmodifiableSet());
        if (idsCandidatos.size() != this.candidatos.size()) {
            throw new IllegalArgumentException("candidateId repetido en el catálogo");
        }
        if (new HashSet<>(this.regiones).size() != this.regiones.size()) {
            throw new IllegalArgumentException("región repetida en el catálogo");
        }
    }

    public boolean existeCandidato(CandidateId candidateId) {
        return idsCandidatos.contains(candidateId);
    }

    public boolean existeRegion(Region region) {
        return regiones.contains(region);
    }

    public List<Candidato> candidatos() {
        return candidatos;
    }

    public List<Region> regiones() {
        return regiones;
    }
}

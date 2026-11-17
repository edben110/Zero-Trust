package com.zerotrust.voterregistration.adapter.persistence;

import com.zerotrust.voterregistration.domain.VotanteRegistrado;
import com.zerotrust.voterregistration.port.RepositorioPadron;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

public class RepositorioPadronJpa implements RepositorioPadron {

    private final VotanteJpaRepository jpa;

    public RepositorioPadronJpa(VotanteJpaRepository jpa) {
        this.jpa = jpa;
    }

    // voter_id es único: los que ya existen se dejan intactos para que la tarea sea idempotente.
    @Override
    @Transactional
    public int guardarNuevos(List<VotanteRegistrado> votantes) {
        Set<String> existentes = jpa.buscarVoterIdsExistentes(
                votantes.stream().map(v -> v.voterId().valor()).toList());
        List<VotanteRegistradoEntity> nuevos = votantes.stream()
                .filter(v -> !existentes.contains(v.voterId().valor()))
                .map(MapperVotante::aEntidad)
                .toList();
        jpa.saveAll(nuevos);
        return nuevos.size();
    }
}

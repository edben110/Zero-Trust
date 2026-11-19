package com.zerotrust.analyticsarchiver.repository;

import com.zerotrust.analyticsarchiver.domain.VotoAuditado;
import com.zerotrust.analyticsarchiver.mapper.MapperVotoAuditado;
import com.zerotrust.analyticsarchiver.port.RepositorioAuditoria;
import org.springframework.transaction.annotation.Transactional;

public class RepositorioAuditoriaJpa implements RepositorioAuditoria {

    private final VotoAuditadoJpaRepository jpa;

    public RepositorioAuditoriaJpa(VotoAuditadoJpaRepository jpa) {
        this.jpa = jpa;
    }

    // vote_id es único: si dos réplicas coincidieran, el reintento encontraría el voto y devolvería false.
    @Override
    @Transactional
    public boolean guardarSiNoExiste(VotoAuditado voto) {
        if (jpa.existsByVoteId(voto.voteId().valor())) {
            return false;
        }
        jpa.save(MapperVotoAuditado.aEntidad(voto));
        return true;
    }
}

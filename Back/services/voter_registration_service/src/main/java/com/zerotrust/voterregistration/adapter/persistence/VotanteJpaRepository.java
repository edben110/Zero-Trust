package com.zerotrust.voterregistration.adapter.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.Set;

public interface VotanteJpaRepository extends JpaRepository<VotanteRegistradoEntity, Long> {

    @Query("select v.voterId from VotanteRegistradoEntity v where v.voterId in :ids")
    Set<String> buscarVoterIdsExistentes(@Param("ids") Collection<String> ids);
}

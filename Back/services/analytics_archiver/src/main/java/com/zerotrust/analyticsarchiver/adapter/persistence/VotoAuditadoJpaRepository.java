package com.zerotrust.analyticsarchiver.adapter.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

public interface VotoAuditadoJpaRepository extends JpaRepository<VotoAuditadoEntity, Long> {

    boolean existsByVoteId(String voteId);
}

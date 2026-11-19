package com.zerotrust.analyticsarchiver.repository;

import com.zerotrust.analyticsarchiver.repository.entity.VotoAuditadoEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VotoAuditadoJpaRepository extends JpaRepository<VotoAuditadoEntity, Long> {

    boolean existsByVoteId(String voteId);
}

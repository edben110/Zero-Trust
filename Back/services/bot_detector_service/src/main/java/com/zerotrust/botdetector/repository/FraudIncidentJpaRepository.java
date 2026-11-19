package com.zerotrust.botdetector.repository;

import com.zerotrust.botdetector.repository.entity.FraudIncidentEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FraudIncidentJpaRepository extends JpaRepository<FraudIncidentEntity, Long> {
}

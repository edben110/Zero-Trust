package com.zerotrust.botdetector.repository;

import com.zerotrust.botdetector.repository.entity.FraudIncidentEntity;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repositorio Spring Data JPA. Desacoplado del dominio mediante el puerto
 * FraudIncidentRepository que el adaptador JpaFraudIncidentRepository implementa.
 */
public interface FraudIncidentJpaRepository extends JpaRepository<FraudIncidentEntity, Long> {
}

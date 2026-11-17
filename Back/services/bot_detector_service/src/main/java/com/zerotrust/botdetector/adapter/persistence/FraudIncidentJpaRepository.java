package com.zerotrust.botdetector.adapter.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repositorio Spring Data JPA. Desacoplado del dominio mediante el puerto
 * FraudIncidentRepository que el adaptador JpaFraudIncidentRepository implementa.
 */
public interface FraudIncidentJpaRepository extends JpaRepository<FraudIncidentEntity, Long> {
}

package com.zerotrust.botdetector.adapter.persistence;

import com.zerotrust.botdetector.domain.model.SecurityAlert;
import com.zerotrust.botdetector.domain.port.FraudIncidentRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Adaptador de persistencia: implementa el puerto del dominio usando JPA.
 * El dominio solo conoce FraudIncidentRepository (la interfaz), nunca esta clase.
 */
@Component
public class JpaFraudIncidentRepository implements FraudIncidentRepository {

    private final FraudIncidentJpaRepository jpaRepository;

    public JpaFraudIncidentRepository(FraudIncidentJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    @Transactional
    public void save(SecurityAlert alert) {
        FraudIncidentEntity entity = new FraudIncidentEntity(
                alert.ip(),
                alert.riskLevel(),
                alert.voteCount(),
                alert.windowSeconds(),
                alert.detectedAt()
        );
        jpaRepository.save(entity);
    }
}

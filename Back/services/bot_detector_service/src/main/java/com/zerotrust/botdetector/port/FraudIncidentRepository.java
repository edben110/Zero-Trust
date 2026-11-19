package com.zerotrust.botdetector.domain.port;

import com.zerotrust.botdetector.domain.model.SecurityAlert;

/**
 * Puerto de salida: persiste incidentes de fraude en PostgreSQL.
 */
public interface FraudIncidentRepository {
    void save(SecurityAlert alert);
}

package com.zerotrust.botdetector.port;

import com.zerotrust.botdetector.domain.SecurityAlert;

/**
 * Puerto de salida: persiste incidentes de fraude en PostgreSQL.
 */
public interface FraudIncidentRepository {
    void save(SecurityAlert alert);
}

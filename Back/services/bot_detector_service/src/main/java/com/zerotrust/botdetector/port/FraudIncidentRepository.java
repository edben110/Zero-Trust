package com.zerotrust.botdetector.port;

import com.zerotrust.botdetector.domain.SecurityAlert;

public interface FraudIncidentRepository {
    void save(SecurityAlert alert);
}

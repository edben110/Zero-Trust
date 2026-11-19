package com.zerotrust.botdetector.port;

import com.zerotrust.botdetector.domain.SecurityAlert;

/**
 * Puerto de salida: publica alertas en el topic security_alerts de Kafka.
 */
public interface SecurityAlertPublisher {
    void publish(SecurityAlert alert);
}

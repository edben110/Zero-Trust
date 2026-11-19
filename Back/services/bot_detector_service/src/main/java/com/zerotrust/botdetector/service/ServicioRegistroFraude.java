package com.zerotrust.botdetector.service;

import com.zerotrust.botdetector.domain.AlertOrigin;
import com.zerotrust.botdetector.domain.FraudKind;
import com.zerotrust.botdetector.domain.SecurityAlert;
import com.zerotrust.botdetector.port.FraudIncidentRepository;
import com.zerotrust.botdetector.port.SecurityAlertPublisher;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;

// No altera el estado de la IP: un doble clic humano no debe bloquear una dirección.
@Service
public class ServicioRegistroFraude {

    private final SecurityAlertPublisher alertPublisher;
    private final FraudIncidentRepository repository;
    private final RegistroContextosIp contextos;
    private final Clock clock;

    public ServicioRegistroFraude(SecurityAlertPublisher alertPublisher, FraudIncidentRepository repository,
                                  RegistroContextosIp contextos, Clock clock) {
        this.alertPublisher = alertPublisher;
        this.repository = repository;
        this.contextos = contextos;
        this.clock = clock;
    }

    public void recordFraud(FraudKind kind, String ip) {
        if (kind == null || ip == null || ip.isBlank()) {
            return;
        }
        Instant ahora = clock.instant();
        SecurityAlert alert = new SecurityAlert(ip, contextos.nivelRiesgo(ip), contextos.votos(ip),
                contextos.ventanaSegundos(), AlertOrigin.LIVE, kind, ahora);
        alertPublisher.publish(alert);
        repository.save(alert);
    }
}

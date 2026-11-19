package com.zerotrust.botdetector.service;

import com.zerotrust.botdetector.domain.AlertOrigin;
import com.zerotrust.botdetector.domain.SecurityAlert;
import com.zerotrust.botdetector.domain.state.IpTrafficContext;
import com.zerotrust.botdetector.exception.ProyeccionNoDisponibleException;
import com.zerotrust.botdetector.port.FraudIncidentRepository;
import com.zerotrust.botdetector.port.RiskStateRepository;
import com.zerotrust.botdetector.port.SecurityAlertPublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;

/**
 * Evalúa cada voto con el patrón State de su IP. Si el estado pide alerta, la publica y la persiste.
 * La regla vive en IpTrafficContext y en los estados; esta clase solo coordina.
 */
@Service
public class ServicioDeteccionBots {

    private static final Logger log = LoggerFactory.getLogger(ServicioDeteccionBots.class);

    private final SecurityAlertPublisher alertPublisher;
    private final FraudIncidentRepository repository;
    private final RiskStateRepository riskStates;
    private final RegistroContextosIp contextos;
    private final Clock clock;

    public ServicioDeteccionBots(SecurityAlertPublisher alertPublisher, FraudIncidentRepository repository,
                                 RiskStateRepository riskStates, RegistroContextosIp contextos, Clock clock) {
        this.alertPublisher = alertPublisher;
        this.repository = repository;
        this.riskStates = riskStates;
        this.contextos = contextos;
        this.clock = clock;
    }

    /** Voto real consumido de raw_votes. */
    public void evaluate(String ip) {
        evaluate(ip, AlertOrigin.LIVE);
    }

    public void evaluate(String ip, AlertOrigin origin) {
        Instant ahora = clock.instant();
        IpTrafficContext context = contextos.contextoDe(ip, ahora);

        if (context.registerVote(ahora)) {
            SecurityAlert alert = new SecurityAlert(ip, context.riskLevel(), context.voteCount(),
                    context.windowSeconds(), origin, ahora);
            alertPublisher.publish(alert);
            repository.save(alert);
        }

        proyectar(ip, context, ahora);
    }

    // La proyección solo alimenta el panel: una caída de PostgreSQL no debe frenar la alerta
    // ni relanzar el voto en el consumidor, que lo contaría dos veces en IpTrafficContext.
    private void proyectar(String ip, IpTrafficContext context, Instant ahora) {
        try {
            riskStates.actualizar(ip, context.riskLevel(), context.voteCount(), context.windowSeconds(), ahora);
        } catch (ProyeccionNoDisponibleException e) {
            log.warn("Estado de la IP {} sin proyectar en ip_risk_state: {}", ip, e.getMessage());
        }
    }
}

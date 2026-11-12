package com.zerotrust.botdetector.service;

import com.zerotrust.botdetector.domain.model.IpTrafficContext;
import com.zerotrust.botdetector.domain.model.SecurityAlert;
import com.zerotrust.botdetector.domain.port.FraudIncidentRepository;
import com.zerotrust.botdetector.domain.port.SecurityAlertPublisher;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Orquesta la detección de bots.
 *
 * Por cada IP mantiene un IpTrafficContext (patrón State).
 * Cuando el contexto señala una alerta, el servicio la publica en Kafka
 * y la persiste en PostgreSQL. Esta clase no contiene lógica de negocio:
 * esa vive en IpTrafficContext y en los estados.
 */
@Service
public class BotDetectorService {

    private final SecurityAlertPublisher  alertPublisher;
    private final FraudIncidentRepository repository;
    private final int                     suspiciousThreshold;
    private final int                     blockedThreshold;
    private final int                     windowSeconds;

    // IP → su contexto de tráfico (acceso concurrente seguro)
    private final ConcurrentMap<String, IpTrafficContext> ipContexts = new ConcurrentHashMap<>();

    public BotDetectorService(
            SecurityAlertPublisher alertPublisher,
            FraudIncidentRepository repository,
            @Value("${detector.suspicious-threshold:5}")  int suspiciousThreshold,
            @Value("${detector.blocked-threshold:10}")    int blockedThreshold,
            @Value("${detector.window-seconds:60}")       int windowSeconds) {
        this.alertPublisher      = alertPublisher;
        this.repository          = repository;
        this.suspiciousThreshold = suspiciousThreshold;
        this.blockedThreshold    = blockedThreshold;
        this.windowSeconds       = windowSeconds;
    }

    /**
     * Evalúa un voto entrante desde la IP dada.
     * Si el estado de la IP requiere alerta, se publica y se persiste.
     */
    public void evaluate(String ip) {
        IpTrafficContext context = ipContexts.computeIfAbsent(ip,
                addr -> new IpTrafficContext(addr, suspiciousThreshold, blockedThreshold, windowSeconds));

        boolean alertNeeded = context.registerVote();

        if (alertNeeded) {
            SecurityAlert alert = new SecurityAlert(
                    ip,
                    context.riskLevel(),
                    context.voteCount(),
                    context.windowSeconds(),
                    Instant.now()
            );
            alertPublisher.publish(alert);
            repository.save(alert);
        }
    }

    /** Expone el nivel de riesgo actual de una IP (para tests y métricas). */
    public String riskLevelOf(String ip) {
        IpTrafficContext ctx = ipContexts.get(ip);
        return ctx == null ? "NORMAL" : ctx.riskLevel();
    }
}

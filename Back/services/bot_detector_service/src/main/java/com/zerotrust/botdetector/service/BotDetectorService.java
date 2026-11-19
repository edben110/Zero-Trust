package com.zerotrust.botdetector.service;

import com.zerotrust.botdetector.domain.exception.ProyeccionNoDisponible;
import com.zerotrust.botdetector.domain.model.AlertOrigin;
import com.zerotrust.botdetector.domain.model.FraudKind;
import com.zerotrust.botdetector.domain.model.IpTrafficContext;
import com.zerotrust.botdetector.domain.model.SecurityAlert;
import com.zerotrust.botdetector.domain.port.FraudIncidentRepository;
import com.zerotrust.botdetector.domain.port.RiskStateRepository;
import com.zerotrust.botdetector.domain.port.SecurityAlertPublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Clock;
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

    private static final Logger log = LoggerFactory.getLogger(BotDetectorService.class);

    private final SecurityAlertPublisher  alertPublisher;
    private final FraudIncidentRepository repository;
    private final RiskStateRepository     riskStates;
    private final int                     suspiciousThreshold;
    private final int                     blockedThreshold;
    private final int                     windowSeconds;
    private final Clock                   clock;

    // IP → su contexto de tráfico (acceso concurrente seguro)
    private final ConcurrentMap<String, IpTrafficContext> ipContexts = new ConcurrentHashMap<>();

    public BotDetectorService(
            SecurityAlertPublisher alertPublisher,
            FraudIncidentRepository repository,
            RiskStateRepository riskStates,
            @Value("${detector.suspicious-threshold:5}")  int suspiciousThreshold,
            @Value("${detector.blocked-threshold:10}")    int blockedThreshold,
            @Value("${detector.window-seconds:60}")       int windowSeconds,
            Clock clock) {
        this.alertPublisher      = alertPublisher;
        this.repository          = repository;
        this.riskStates          = riskStates;
        this.suspiciousThreshold = suspiciousThreshold;
        this.blockedThreshold    = blockedThreshold;
        this.windowSeconds       = windowSeconds;
        this.clock               = clock;
    }

    /** Voto real consumido de raw_votes. */
    public void evaluate(String ip) {
        evaluate(ip, AlertOrigin.LIVE);
    }

    /**
     * Evalúa un voto entrante desde la IP dada.
     * Si el estado de la IP requiere alerta, se publica y se persiste.
     */
    public void evaluate(String ip, AlertOrigin origin) {
        Instant ahora = clock.instant();
        IpTrafficContext context = ipContexts.computeIfAbsent(ip,
                addr -> new IpTrafficContext(addr, suspiciousThreshold, blockedThreshold, windowSeconds, ahora));

        boolean alertNeeded = context.registerVote(ahora);

        if (alertNeeded) {
            SecurityAlert alert = new SecurityAlert(
                    ip,
                    context.riskLevel(),
                    context.voteCount(),
                    context.windowSeconds(),
                    origin,
                    ahora
            );
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
        } catch (ProyeccionNoDisponible e) {
            log.warn("Estado de la IP {} sin proyectar en ip_risk_state: {}", ip, e.getMessage());
        }
    }

    /**
     * Intervención administrativa: descarta el contexto en memoria y deja la IP
     * en NORMAL. Aplica incluso a una IP BLOQUEADA (estado terminal).
     */
    public void reset(String ip) {
        if (ip == null || ip.isBlank()) {
            throw new IllegalArgumentException("ip no puede ser nula ni vacía");
        }
        ipContexts.remove(ip);
        riskStates.actualizar(ip, "NORMAL", 0, windowSeconds, clock.instant());
    }

    /**
     * Registra un incidente de fraude electoral detectado por voting_api
     * (voto duplicado o voto anulado por región incorrecta).
     *
     * No altera el patrón State de la IP: un doble clic humano no debe
     * bloquear una dirección. Solo se publica la alerta y se persiste el
     * incidente con su tipo.
     */
    public void recordFraud(FraudKind kind, String ip) {
        if (kind == null || ip == null || ip.isBlank()) {
            return;
        }
        Instant ahora = clock.instant();
        SecurityAlert alert = new SecurityAlert(
                ip,
                riskLevelOf(ip),
                voteCountOf(ip),
                windowSeconds,
                AlertOrigin.LIVE,
                kind,
                ahora
        );
        alertPublisher.publish(alert);
        repository.save(alert);
    }

    /**
     * Purga administrativa: olvida todos los contextos en memoria. La fila de
     * ip_risk_state la elimina el panel en PostgreSQL.
     */
    public void resetAll() {
        ipContexts.clear();
    }

    /** Expone el nivel de riesgo actual de una IP (para tests y métricas). */
    public String riskLevelOf(String ip) {
        IpTrafficContext ctx = ipContexts.get(ip);
        return ctx == null ? "NORMAL" : ctx.riskLevel();
    }

    public int voteCountOf(String ip) {
        IpTrafficContext ctx = ipContexts.get(ip);
        return ctx == null ? 0 : ctx.voteCount();
    }
}

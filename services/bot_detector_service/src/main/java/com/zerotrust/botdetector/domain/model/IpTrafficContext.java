package com.zerotrust.botdetector.domain.model;

import com.zerotrust.botdetector.domain.state.IpRiskState;
import com.zerotrust.botdetector.domain.state.NormalState;

/**
 * Entidad de dominio que mantiene el estado de tráfico de una IP.
 *
 * Es el "contexto" del patrón State. Los estados acceden a él para leer
 * contadores y solicitar transiciones, sin acoplar la lógica de negocio
 * al código que gestiona la IP.
 *
 * Los invariantes (umbrales, conteo) están protegidos: no hay setters públicos,
 * solo métodos semánticos.
 */
public final class IpTrafficContext {

    private final String       ip;
    private final int          suspiciousThreshold;
    private final int          blockedThreshold;
    private final int          windowSeconds;

    private IpRiskState currentState;
    private int         voteCount;
    private boolean     alertRequested;

    public IpTrafficContext(String ip, int suspiciousThreshold, int blockedThreshold, int windowSeconds) {
        if (ip == null || ip.isBlank()) {
            throw new IllegalArgumentException("ip no puede ser nula ni vacía");
        }
        if (suspiciousThreshold <= 0 || blockedThreshold <= suspiciousThreshold) {
            throw new IllegalArgumentException(
                "blockedThreshold debe ser mayor que suspiciousThreshold y ambos > 0");
        }
        this.ip                  = ip;
        this.suspiciousThreshold = suspiciousThreshold;
        this.blockedThreshold    = blockedThreshold;
        this.windowSeconds       = windowSeconds;
        this.currentState        = new NormalState();
        this.voteCount           = 0;
        this.alertRequested      = false;
    }

    // ── Métodos de negocio usados por los estados ──────────────────────────────

    public void incrementVoteCount() {
        voteCount++;
    }

    public void transitionTo(IpRiskState newState) {
        this.currentState = newState;
    }

    public void requestAlert() {
        this.alertRequested = true;
    }

    // ── Punto de entrada del patrón State ─────────────────────────────────────

    /**
     * Registra un nuevo voto desde esta IP y delega la evaluación al estado actual.
     * Resetea la bandera de alerta antes de cada evaluación.
     *
     * @return true si el estado actual requiere emitir una alerta.
     */
    public boolean registerVote() {
        alertRequested = false;
        currentState.onVoteReceived(this);
        return alertRequested;
    }

    // ── Accesores de solo lectura ──────────────────────────────────────────────

    public String ip()                  { return ip; }
    public int    voteCount()           { return voteCount; }
    public int    suspiciousThreshold() { return suspiciousThreshold; }
    public int    blockedThreshold()    { return blockedThreshold; }
    public int    windowSeconds()       { return windowSeconds; }
    public String riskLevel()           { return currentState.name(); }
}

package com.zerotrust.botdetector.domain.state;

import java.time.Instant;

/**
 * Entidad de dominio que mantiene el estado de tráfico de una IP.
 *
 * Es el "contexto" del patrón State. Los estados acceden a él para leer
 * contadores y solicitar transiciones, sin acoplar la lógica de negocio
 * al código que gestiona la IP.
 *
 * Los invariantes (umbrales, conteo, ventana) están protegidos: no hay setters
 * públicos, solo métodos semánticos.
 *
 * La ventana de tiempo evita que una IP quede marcada para siempre por unos
 * pocos votos aislados. Al expirar, NORMAL/SOSPECHOSA vuelven a cero. BLOQUEADA
 * es terminal y solo se libera con una intervención administrativa.
 */
public final class IpTrafficContext {

    private final String  ip;
    private final int     suspiciousThreshold;
    private final int     blockedThreshold;
    private final int     windowSeconds;

    private IpRiskState currentState;
    private int         voteCount;
    private boolean     alertRequested;
    private Instant     windowStart;

    public IpTrafficContext(String ip, int suspiciousThreshold, int blockedThreshold, int windowSeconds) {
        this(ip, suspiciousThreshold, blockedThreshold, windowSeconds, Instant.now());
    }

    public IpTrafficContext(String ip, int suspiciousThreshold, int blockedThreshold, int windowSeconds,
                            Instant now) {
        if (ip == null || ip.isBlank()) {
            throw new IllegalArgumentException("ip no puede ser nula ni vacía");
        }
        if (suspiciousThreshold <= 0 || blockedThreshold <= suspiciousThreshold) {
            throw new IllegalArgumentException(
                "blockedThreshold debe ser mayor que suspiciousThreshold y ambos > 0");
        }
        if (windowSeconds <= 0) {
            throw new IllegalArgumentException("windowSeconds debe ser mayor que 0");
        }
        if (now == null) {
            throw new IllegalArgumentException("now no puede ser nulo");
        }
        this.ip                  = ip;
        this.suspiciousThreshold = suspiciousThreshold;
        this.blockedThreshold    = blockedThreshold;
        this.windowSeconds       = windowSeconds;
        this.currentState        = new NormalState();
        this.voteCount           = 0;
        this.alertRequested      = false;
        this.windowStart         = now;
    }

    // ── Solo para los estados del paquete: nadie fuera puede forzar una transición ──

    void incrementVoteCount() {
        voteCount++;
    }

    void transitionTo(IpRiskState newState) {
        if (newState == null) {
            throw new IllegalArgumentException("newState no puede ser nulo");
        }
        this.currentState = newState;
    }

    void requestAlert() {
        this.alertRequested = true;
    }

    // ── Punto de entrada del patrón State ────────────────────────────────────

    /**
     * Registra un nuevo voto desde esta IP y delega la evaluación al estado actual.
     * Resetea la bandera de alerta antes de cada evaluación.
     *
     * @return true si el estado actual requiere emitir una alerta.
     */
    public boolean registerVote() {
        return registerVote(Instant.now());
    }

    /**
     * Variante determinista usada por el servicio (reloj inyectable) y los tests.
     */
    public boolean registerVote(Instant now) {
        if (now == null) {
            throw new IllegalArgumentException("now no puede ser nulo");
        }
        reiniciarVentanaSiExpiro(now);
        alertRequested = false;
        currentState.onVoteReceived(this);
        return alertRequested;
    }

    private void reiniciarVentanaSiExpiro(Instant now) {
        if (currentState.esFinal()) {
            return;
        }
        if (!now.isBefore(windowStart.plusSeconds(windowSeconds))) {
            voteCount    = 0;
            currentState = new NormalState();
            windowStart  = now;
        }
    }

    // ── Accesores de solo lectura ────────────────────────────────────────────

    public String  ip()                  { return ip; }
    public int     voteCount()           { return voteCount; }
    public int     suspiciousThreshold() { return suspiciousThreshold; }
    public int     blockedThreshold()    { return blockedThreshold; }
    public int     windowSeconds()       { return windowSeconds; }
    public String  riskLevel()           { return currentState.name(); }
    public Instant windowStart()         { return windowStart; }
}

package com.zerotrust.botdetector.domain.state;

import java.time.Instant;

// La ventana evita que unos votos aislados marquen la IP para siempre; BLOQUEADA no expira.
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

    // Solo para los estados del paquete: nadie fuera puede forzar una transición.

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

    public boolean registerVote() {
        return registerVote(Instant.now());
    }

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

    public String  ip()                  { return ip; }
    public int     voteCount()           { return voteCount; }
    public int     suspiciousThreshold() { return suspiciousThreshold; }
    public int     blockedThreshold()    { return blockedThreshold; }
    public int     windowSeconds()       { return windowSeconds; }
    public String  riskLevel()           { return currentState.name(); }
    public Instant windowStart()         { return windowStart; }
}

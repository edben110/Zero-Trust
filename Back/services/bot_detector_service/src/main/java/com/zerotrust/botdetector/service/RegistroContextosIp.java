package com.zerotrust.botdetector.service;

import com.zerotrust.botdetector.domain.state.IpTrafficContext;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Contextos de tráfico por IP (patrón State) que comparten los servicios del detector.
 * Vive en memoria del proceso: un reinicio deja todas las IPs en NORMAL.
 */
@Component
public class RegistroContextosIp {

    private static final String NIVEL_SIN_CONTEXTO = "NORMAL";

    private final int suspiciousThreshold;
    private final int blockedThreshold;
    private final int windowSeconds;

    // IP → su contexto de tráfico (acceso concurrente seguro)
    private final ConcurrentMap<String, IpTrafficContext> contextos = new ConcurrentHashMap<>();

    public RegistroContextosIp(@Value("${detector.suspicious-threshold:5}") int suspiciousThreshold,
                               @Value("${detector.blocked-threshold:10}") int blockedThreshold,
                               @Value("${detector.window-seconds:60}") int windowSeconds) {
        this.suspiciousThreshold = suspiciousThreshold;
        this.blockedThreshold = blockedThreshold;
        this.windowSeconds = windowSeconds;
    }

    /** Devuelve el contexto de la IP y lo crea, con la ventana empezando ahora, si no existe. */
    public IpTrafficContext contextoDe(String ip, Instant ahora) {
        return contextos.computeIfAbsent(ip,
                addr -> new IpTrafficContext(addr, suspiciousThreshold, blockedThreshold, windowSeconds, ahora));
    }

    public String nivelRiesgo(String ip) {
        IpTrafficContext contexto = contextos.get(ip);
        return contexto == null ? NIVEL_SIN_CONTEXTO : contexto.riskLevel();
    }

    public int votos(String ip) {
        IpTrafficContext contexto = contextos.get(ip);
        return contexto == null ? 0 : contexto.voteCount();
    }

    public int ventanaSegundos() {
        return windowSeconds;
    }

    public void olvidar(String ip) {
        contextos.remove(ip);
    }

    public void olvidarTodos() {
        contextos.clear();
    }
}

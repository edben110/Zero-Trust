package com.zerotrust.botdetector.service;

import com.zerotrust.botdetector.port.RiskStateRepository;
import org.springframework.stereotype.Service;

import java.time.Clock;

/**
 * Intervenciones del panel de administración sobre el estado de las IPs.
 */
@Service
public class ServicioAdministracionIps {

    private static final String NIVEL_INICIAL = "NORMAL";

    private final RiskStateRepository riskStates;
    private final RegistroContextosIp contextos;
    private final Clock clock;

    public ServicioAdministracionIps(RiskStateRepository riskStates, RegistroContextosIp contextos, Clock clock) {
        this.riskStates = riskStates;
        this.contextos = contextos;
        this.clock = clock;
    }

    /**
     * Descarta el contexto en memoria y deja la IP en NORMAL. Aplica incluso a una IP BLOQUEADA (estado terminal).
     */
    public void reset(String ip) {
        if (ip == null || ip.isBlank()) {
            throw new IllegalArgumentException("ip no puede ser nula ni vacía");
        }
        contextos.olvidar(ip);
        riskStates.actualizar(ip, NIVEL_INICIAL, 0, contextos.ventanaSegundos(), clock.instant());
    }

    /**
     * Purga administrativa: olvida todos los contextos en memoria. La fila de ip_risk_state la elimina el panel.
     */
    public void resetAll() {
        contextos.olvidarTodos();
    }

    public String riskLevelOf(String ip) {
        return contextos.nivelRiesgo(ip);
    }

    public int voteCountOf(String ip) {
        return contextos.votos(ip);
    }
}

package com.zerotrust.botdetector.service;

import com.zerotrust.botdetector.port.RiskStateRepository;
import org.springframework.stereotype.Service;

import java.time.Clock;

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

    // Aplica incluso a una IP BLOQUEADA, que es terminal.
    public void reset(String ip) {
        if (ip == null || ip.isBlank()) {
            throw new IllegalArgumentException("ip no puede ser nula ni vacía");
        }
        contextos.olvidar(ip);
        riskStates.actualizar(ip, NIVEL_INICIAL, 0, contextos.ventanaSegundos(), clock.instant());
    }

    // La fila de ip_risk_state la borra el panel en PostgreSQL.
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

package com.zerotrust.analyticsarchiver.config;

import org.springframework.util.backoff.FixedBackOff;

// El DefaultErrorHandler de Spring Kafka descarta el mensaje tras 10 intentos; aquí se reintenta sin límite
// para no confirmar el offset de un voto que todavía no está en vote_audit.
public final class PoliticaReintentos {

    private static final long INTERVALO_MS = 5_000L;

    private PoliticaReintentos() {
    }

    public static FixedBackOff mientrasFalleLaBaseDeDatos() {
        return new FixedBackOff(INTERVALO_MS, FixedBackOff.UNLIMITED_ATTEMPTS);
    }
}

package com.zerotrust.analyticsarchiver.config;

import org.springframework.util.backoff.FixedBackOff;

// Sin límite: el DefaultErrorHandler descartaría el voto tras 10 intentos y confirmaría su offset.
public final class PoliticaReintentos {

    private static final long INTERVALO_MS = 5_000L;

    private PoliticaReintentos() {
    }

    public static FixedBackOff mientrasFalleLaBaseDeDatos() {
        return new FixedBackOff(INTERVALO_MS, FixedBackOff.UNLIMITED_ATTEMPTS);
    }
}

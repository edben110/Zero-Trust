package com.zerotrust.analyticsarchiver.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.util.backoff.FixedBackOff;

import static org.assertj.core.api.Assertions.assertThat;

class PoliticaReintentosTest {

    @Test
    @DisplayName("La política de reintentos no tiene límite, para no confirmar offsets mientras la BD falle")
    void reintentosSinLimite() {
        FixedBackOff politica = PoliticaReintentos.mientrasFalleLaBaseDeDatos();

        assertThat(politica.getMaxAttempts()).isEqualTo(FixedBackOff.UNLIMITED_ATTEMPTS);
        assertThat(politica.getInterval()).isPositive();
    }
}

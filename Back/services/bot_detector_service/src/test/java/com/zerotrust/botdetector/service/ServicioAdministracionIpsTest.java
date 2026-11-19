package com.zerotrust.botdetector.service;

import com.zerotrust.botdetector.port.RiskStateRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class ServicioAdministracionIpsTest {

    private static final Instant AHORA = Instant.parse("2026-11-18T12:00:00Z");
    private static final Clock RELOJ = Clock.fixed(AHORA, ZoneOffset.UTC);

    @Mock
    private RiskStateRepository riskStates;

    private final RegistroContextosIp registro = new RegistroContextosIp(2, 4, 60);

    private ServicioAdministracionIps svc;

    @BeforeEach
    void preparar() {
        svc = new ServicioAdministracionIps(riskStates, registro, RELOJ);
    }

    private void votar(String ip, int votos) {
        for (int i = 0; i < votos; i++) {
            registro.contextoDe(ip, AHORA).registerVote(AHORA);
        }
    }

    @Test
    @DisplayName("Reset debe devolver una IP BLOQUEADA a NORMAL y proyectarlo")
    void resetDevuelveLaIpBloqueadaANormal() {
        // Arrange: 5 votos con umbral bloqueada 4 → BLOQUEADA
        votar("5.5.5.5", 5);
        assertEquals("BLOQUEADA", svc.riskLevelOf("5.5.5.5"));

        // Act
        svc.reset("5.5.5.5");

        // Assert
        assertEquals("NORMAL", svc.riskLevelOf("5.5.5.5"));
        assertEquals(0, svc.voteCountOf("5.5.5.5"));
        verify(riskStates).actualizar("5.5.5.5", "NORMAL", 0, 60, AHORA);
    }

    @Test
    @DisplayName("Reset debe rechazar una IP vacía sin proyectar nada")
    void resetRechazaIpVacia() {
        // Act + Assert
        assertThrows(IllegalArgumentException.class, () -> svc.reset(" "));
        assertThrows(IllegalArgumentException.class, () -> svc.reset(null));
        verifyNoInteractions(riskStates);
    }

    @Test
    @DisplayName("ResetAll debe olvidar todos los contextos en memoria")
    void resetAllOlvidaTodosLosContextosEnMemoria() {
        // Arrange
        votar("7.7.7.7", 3);
        assertEquals(3, svc.voteCountOf("7.7.7.7"));

        // Act
        svc.resetAll();

        // Assert
        assertEquals("NORMAL", svc.riskLevelOf("7.7.7.7"));
        assertEquals(0, svc.voteCountOf("7.7.7.7"));
        verifyNoInteractions(riskStates);
    }
}

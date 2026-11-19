package com.zerotrust.botdetector.domain;

import com.zerotrust.botdetector.domain.state.IpTrafficContext;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.time.Instant;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * Desde fuera del paquete state nadie puede forzar una transición ni pedir una alerta:
 * el único camino es registerVote, que aplica las reglas de cada estado.
 */
class EncapsulacionStateTest {

    @Test
    @DisplayName("IpTrafficContext solo debe exponer registerVote y accesores de solo lectura")
    void soloExponeRegisterVoteYLectura() {
        // Arrange
        Set<String> esperados = Set.of("registerVote", "ip", "voteCount", "suspiciousThreshold",
                "blockedThreshold", "windowSeconds", "riskLevel", "windowStart");

        // Act
        Set<String> publicos = Arrays.stream(IpTrafficContext.class.getDeclaredMethods())
                .filter(m -> Modifier.isPublic(m.getModifiers()))
                .map(Method::getName)
                .collect(Collectors.toSet());

        // Assert
        assertEquals(esperados, publicos);
    }

    @ParameterizedTest
    @ValueSource(strings = {"IpRiskState", "NormalState", "SuspiciousState", "BlockedState"})
    @DisplayName("Los estados no deben ser visibles fuera del paquete state")
    void estadosSonDelPaquete(String estado) throws ClassNotFoundException {
        // Act
        Class<?> clase = Class.forName("com.zerotrust.botdetector.domain.state." + estado);

        // Assert
        assertFalse(Modifier.isPublic(clase.getModifiers()));
    }

    @Test
    @DisplayName("La única forma de llegar a BLOQUEADA debe ser registrar votos")
    void llegaABloqueadaRegistrandoVotos() {
        // Arrange
        Instant t0 = Instant.parse("2026-11-18T12:00:00Z");
        IpTrafficContext contexto = new IpTrafficContext("10.0.0.9", 1, 2, 60, t0);

        // Act
        contexto.registerVote(t0);
        contexto.registerVote(t0.plusSeconds(1));
        contexto.registerVote(t0.plusSeconds(2));

        // Assert
        assertEquals("BLOQUEADA", contexto.riskLevel());
    }
}

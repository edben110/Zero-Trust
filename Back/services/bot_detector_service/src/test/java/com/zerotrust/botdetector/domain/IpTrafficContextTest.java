package com.zerotrust.botdetector.domain;

import com.zerotrust.botdetector.domain.model.IpTrafficContext;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pruebas de dominio puro. Sin Spring, sin Kafka, sin BD.
 * Verifica las transiciones de estado del patrón State.
 */
class IpTrafficContextTest {

    private IpTrafficContext context(String ip) {
        // umbral sospechosa: 3, umbral bloqueada: 6, ventana: 60s
        return new IpTrafficContext(ip, 3, 6, 60);
    }

    @Test
    void estadoInicialEsNormal() {
        IpTrafficContext ctx = context("10.0.0.1");
        assertEquals("NORMAL", ctx.riskLevel());
    }

    @Test
    void noGeneraAlertaEnEstadoNormalBajoUmbral() {
        // Arrange
        IpTrafficContext ctx = context("10.0.0.1");

        // Act & Assert: 3 votos, aún no supera el umbral
        for (int i = 0; i < 3; i++) {
            assertFalse(ctx.registerVote());
        }
        assertEquals("NORMAL", ctx.riskLevel());
    }

    @Test
    void transicionaNormalASospechosaAlSuperarUmbral() {
        // Arrange
        IpTrafficContext ctx = context("10.0.0.2");

        // Act: 4 votos superan el umbral (3)
        ctx.registerVote();
        ctx.registerVote();
        ctx.registerVote();
        boolean alert = ctx.registerVote(); // 4to → transición

        // Assert
        assertTrue(alert);
        assertEquals("SOSPECHOSA", ctx.riskLevel());
    }

    @Test
    void generaAlertaEnCadaVotoEstadoSospechosa() {
        // Arrange
        IpTrafficContext ctx = context("10.0.0.3");
        for (int i = 0; i < 4; i++) ctx.registerVote(); // lleva a SOSPECHOSA

        // Act
        boolean alert = ctx.registerVote(); // 5to, ya sospechosa

        // Assert
        assertTrue(alert);
        assertEquals("SOSPECHOSA", ctx.riskLevel());
    }

    @Test
    void transicionaSospechosaABloqueadaAlSuperarUmbralCritico() {
        // Arrange
        IpTrafficContext ctx = context("10.0.0.4");
        for (int i = 0; i < 6; i++) ctx.registerVote(); // 6 votos → umbral bloqueada

        // Act
        boolean alert = ctx.registerVote(); // 7mo → BLOQUEADA

        // Assert
        assertTrue(alert);
        assertEquals("BLOQUEADA", ctx.riskLevel());
    }

    @Test
    void permaneceBloquedaYSigueGenerandoAlertas() {
        // Arrange
        IpTrafficContext ctx = context("10.0.0.5");
        for (int i = 0; i < 7; i++) ctx.registerVote(); // lleva a BLOQUEADA

        // Act
        boolean alert = ctx.registerVote(); // 8vo, ya bloqueada

        // Assert
        assertTrue(alert);
        assertEquals("BLOQUEADA", ctx.riskLevel());
    }

    @Test
    void ipNulaLanzaExcepcion() {
        assertThrows(IllegalArgumentException.class, () -> new IpTrafficContext(null, 3, 6, 60));
    }

    @Test
    void umbralBloqueadaMenorQueSospechosaLanzaExcepcion() {
        assertThrows(IllegalArgumentException.class,
                () -> new IpTrafficContext("10.0.0.1", 10, 5, 60));
    }
}
